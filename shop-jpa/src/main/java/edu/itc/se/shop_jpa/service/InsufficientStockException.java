package edu.itc.se.shop_jpa.service;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long id, int currentStock, int requestedRemoval) {
        super("Product " + id + " has " + currentStock + " units, cannot remove " + requestedRemoval);
    }
}
