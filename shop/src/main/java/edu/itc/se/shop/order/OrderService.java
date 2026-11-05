package edu.itc.se.shop.order;

import edu.itc.se.shop.audit.Audited;
import edu.itc.se.shop.common.BadRequestException;
import edu.itc.se.shop.common.ConflictException;
import edu.itc.se.shop.common.NotFoundException;
import edu.itc.se.shop.common.Page;
import edu.itc.se.shop.config.ShopProperties;
import edu.itc.se.shop.customer.CustomerRepository;
import edu.itc.se.shop.service.Notifier;
import edu.itc.se.shop.product.Product;
import edu.itc.se.shop.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final ShopProperties props;
    private final Notifier notifier;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products, CustomerRepository customers,
                        ShopProperties props, Notifier notifier, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.customers = customers;
        this.props = props;
        this.notifier = notifier;
        this.clock = clock;
    }

    /**
     * The transaction boundary: every stock decrement and the order insert commit together.
     * Any RuntimeException (e.g. InsufficientStockException) rolls back all of them.
     */
    @Transactional
    @Audited
    public OrderDto place(PlaceOrder cmd) {
        // 1. validate: no duplicate productId
        Set<Long> seen = new HashSet<>();
        for (PlaceOrder.Line line : cmd.lines()) {
            if (!seen.add(line.productId())) {
                throw new BadRequestException("Duplicate product " + line.productId());
            }
        }
        if (!customers.exists(cmd.customerId())) {
            throw new NotFoundException("Customer " + cmd.customerId() + " not found");
        }

        // 2. reserve stock line by line and snapshot the unit price
        List<OrderLine> lines = new ArrayList<>();
        for (PlaceOrder.Line line : cmd.lines()) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product " + line.productId() + " not found"));
            if (products.updateStock(line.productId(), -line.qty()) == 0) {
                throw new InsufficientStockException(line.productId());   // -> rollback of earlier lines
            }
            lines.add(new OrderLine(product.id(), line.qty(), product.price()));
        }

        // 3. total = net x (1 + taxRate), HALF_UP, 2 decimals
        BigDecimal total = Pricing.total(lines, props.taxRate());

        // 4. persist
        Order order = new Order(null, cmd.customerId(), OrderStatus.NEW, LocalDateTime.now(clock), total);
        long id = orders.insert(order, lines);

        // 5. result
        return OrderDto.from(new Order(id, order.customerId(), order.status(), order.placedAt(), total), lines);
    }

    @Transactional
    @Audited
    public OrderDto cancel(long id) {
        Order order = orders.findById(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
        if (order.status() != OrderStatus.NEW) {
            throw new ConflictException("Order " + id + " is " + order.status() + "; only NEW orders can be cancelled");
        }
        // compare-and-set protects against two concurrent cancels restoring stock twice
        if (orders.updateStatus(id, OrderStatus.NEW, OrderStatus.CANCELLED) == 0) {
            throw new ConflictException("Order " + id + " was modified concurrently");
        }
        List<OrderLine> lines = orders.findLines(id);
        for (OrderLine l : lines) {
            products.updateStock(l.productId(), l.qty());
        }
        notifier.send("customer-" + order.customerId(), "Your order " + id + " was cancelled");

        Order cancelled = new Order(order.id(), order.customerId(), OrderStatus.CANCELLED, order.placedAt(), order.total());
        return OrderDto.from(cancelled, lines);
    }

    @Transactional(readOnly = true)
    public OrderDto get(long id) {
        Order order = orders.findById(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
        return OrderDto.from(order, orders.findLines(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> list(Long customerId, OrderStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("page must be >= 0 and size must be between 1 and 100");
        }
        long total = orders.count(customerId, status);
        List<Order> rows = orders.find(customerId, status, size, (long) page * size);
        Map<Long, List<OrderLine>> lines = orders.findLinesByOrderIds(rows.stream().map(Order::id).toList());
        List<OrderDto> content = rows.stream()
                .map(o -> OrderDto.from(o, lines.getOrDefault(o.id(), List.of())))
                .toList();
        return new Page<>(content, page, size, total);
    }
}