package edu.itc.se.shop_jpa.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderDto;
import edu.itc.se.shop_jpa.web.dto.OrderLineDto;

@Service
public class CustomerOrderService {

    private final OrderRepository orders;

    public CustomerOrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> ordersFor(String email) {
        return orders.findWithLinesByCustomerEmail(email).stream()
                .map(o -> new OrderDto(o.getId(), o.getStatus(), o.getPlacedAt(),
                        o.getLines().stream()
                                .map(l -> new OrderLineDto(
                                        l.getProduct().getSku(),
                                        l.getProduct().getName(),
                                        l.getQuantity(),
                                        l.getProduct().getPrice().getAmount()))
                                .toList()))
                .toList();
    }
}