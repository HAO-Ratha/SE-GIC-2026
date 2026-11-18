package edu.itc.se.shop_jpa.domain;

import java.time.Instant;

@Deprecated
public class ShopOrder extends Order {
    public ShopOrder(Customer customer) {
        super(customer, OrderStatus.NEW, Instant.now());
    }
}