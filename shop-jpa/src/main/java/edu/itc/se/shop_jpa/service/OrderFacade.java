package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.OrderStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class OrderFacade {

    private final OrderService orderService;

    public OrderFacade(OrderService orderService) {
        this.orderService = orderService;
    }

    public void changeStatusWithRetry(Long id, OrderStatus next) {
        for (int attempt = 1; ; attempt++) {
            try {
                orderService.changeStatus(id, next);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == 3) {
                    throw e;
                }
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}