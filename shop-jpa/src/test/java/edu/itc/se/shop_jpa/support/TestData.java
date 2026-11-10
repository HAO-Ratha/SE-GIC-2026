package edu.itc.se.shop_jpa.support;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestData {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public TestData(CustomerRepository customers, ProductRepository products, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public void paidOrders(int count, int linesPerOrder) {
        if (orders.countByStatus(OrderStatus.PAID) >= count) {
            return;
        }

        Customer customer = customers.findByEmail("demo@itc.edu")
                .orElseGet(() -> customers.save(new Customer("Demo User", "demo@itc.edu")));

        List<Product> productPool = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            productPool.add(products.save(new Product(
                    "P" + i,
                    "Product " + i,
                    new Money(new BigDecimal("10.00"), "EUR"),
                    Category.HARDWARE,
                    100
            )));
        }

        for (int i = 0; i < count; i++) {
            Order order = new Order(customer, OrderStatus.PAID, Instant.now().minusSeconds(i * 60L));
            for (int j = 0; j < linesPerOrder; j++) {
                Product product = productPool.get((i + j) % productPool.size());
                order.addLine(product, j + 1);
            }
            orders.save(order);
        }
    }
}
