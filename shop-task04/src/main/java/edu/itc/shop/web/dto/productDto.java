package edu.itc.shop.web.dto;

import edu.itc.shop.domain.Product;
import java.math.BigDecimal;

public record productDto(
        long id,
        String name,
        BigDecimal price,
        int stock
) {

    public static productDto from(Product product) {
        return new productDto(
                product.id(),
                product.name(),
                product.price(),
                product.stock()
        );
    }
}