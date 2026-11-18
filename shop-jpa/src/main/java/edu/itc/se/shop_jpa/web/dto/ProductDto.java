// ProductDto.java
// Represents the product data returned in JSON responses:

package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;
import edu.itc.se.shop_jpa.domain.Product;

public record ProductDto(
        long id,
        String name,
        BigDecimal price,
        int stock
) {
    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice().getAmount(),
                product.getStock()
        );
    }
}
