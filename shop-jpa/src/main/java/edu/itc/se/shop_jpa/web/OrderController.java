package edu.itc.se.shop_jpa.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.service.OrderService;
import edu.itc.se.shop_jpa.service.OrderFeedService;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;
import edu.itc.se.shop_jpa.web.dto.OrderFeedResponse;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orders;
    private final OrderService orderService;
    private final OrderFeedService feedService;

    public OrderController(OrderRepository orders, OrderService orderService, OrderFeedService feedService) {
        this.orders = orders;
        this.orderService = orderService;
        this.feedService = feedService;
    }

    @GetMapping
    public Page<OrderSummary> list(@RequestParam OrderStatus status,
                                   @PageableDefault(size = 20, sort = "placedAt",
                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return orders.findByStatus(status, pageable);
    }

    @GetMapping("/{orderId}/line-names")
    public List<String> lineNames(@PathVariable Long orderId) {
        return orderService.getLineNames(orderId);
    }

    @GetMapping("/feed")
    public OrderFeedResponse feed(@RequestParam(required = false) String after,
                                  @RequestParam(defaultValue = "20") int size) {
        try {
            return feedService.slice(after, size);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/feed/keyset")
    public OrderFeedResponse keysetFeed(@RequestParam(required = false) String after,
                                        @RequestParam(defaultValue = "20") int size) {
        try {
            return feedService.keyset(after, size);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}