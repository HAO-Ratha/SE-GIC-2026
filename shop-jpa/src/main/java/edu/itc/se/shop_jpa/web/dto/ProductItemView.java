package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;

public record ProductItemView(
        long id,
        String name,
        BigDecimal price,
        BigDecimal gross,
        int stock
) {}