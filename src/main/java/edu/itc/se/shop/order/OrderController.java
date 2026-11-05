package edu.itc.se.shop.order;

import edu.itc.se.shop.common.Page;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderDto> place(@Valid @RequestBody PlaceOrder cmd) {
        OrderDto dto = service.place(cmd);
        return ResponseEntity.created(URI.create("/api/orders/" + dto.id())).body(dto);
    }

    @GetMapping
    public Page<OrderDto> list(@RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) OrderStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return service.list(customerId, status, page, size);
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable long id) {
        return service.cancel(id);
    }
}