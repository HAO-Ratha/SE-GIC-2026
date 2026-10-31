package edu.itc.se.shop.order;

import java.math.BigDecimal;

/** A line of an order. unitPrice is a snapshot of the product price at the moment of purchase. */
public record OrderLine(long productId, int qty, BigDecimal unitPrice) {}