package edu.itc.se.shop_jpa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public void markPaid(Long orderId) {

        // Find the order
        Order order = orders.findById(orderId)
                .orElseThrow();

        // Change its status
        order.setStatus(OrderStatus.PAID);

        // Do not call save()
    }
}