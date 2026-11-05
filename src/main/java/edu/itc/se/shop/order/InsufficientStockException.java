package edu.itc.se.shop.order;

import edu.itc.se.shop.common.ConflictException;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(long productId) {
        super("Insufficient stock for product " + productId);
    }
}