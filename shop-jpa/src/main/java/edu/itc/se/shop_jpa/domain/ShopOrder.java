package edu.itc.se.shop_jpa.domain;

import java.time.Instant;

public class ShopOrder extends Order {

    protected ShopOrder() {
        super();
    }

    public ShopOrder(Customer customer) {
        super(customer, OrderStatus.NEW, Instant.now());
    }
}