package edu.itc.se.shop_jpa.web.dto;

import java.time.Instant;
import edu.itc.se.shop_jpa.domain.OrderStatus;

public interface OrderSummary {
    Long getId();
    OrderStatus getStatus();
    Instant getPlacedAt();
    CustomerName getCustomer();

    interface CustomerName {
        String getName();
    }
}