package edu.itc.se.shop.product;

import java.math.BigDecimal;

public record Product(long id, String name, BigDecimal price, int stock) {}