package edu.itc.shop.domain;

import java.math.BigDecimal;

public record Product(
        long id,
        String name,
        BigDecimal price,
        int stock
) {
}