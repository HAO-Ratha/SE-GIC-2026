package edu.itc.se.shop_jpa.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.itc.se.shop_jpa.service.CustomerOrderService;
import edu.itc.se.shop_jpa.web.dto.OrderDto;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerOrderService service;

    public CustomerController(CustomerOrderService service) {
        this.service = service;
    }

    // ":.+" keeps the ".edu" part of the email from being cut off as a file extension
    @GetMapping("/{email:.+}/orders")
    public List<OrderDto> orders(@PathVariable String email) {
        return service.ordersFor(email);
    }
}