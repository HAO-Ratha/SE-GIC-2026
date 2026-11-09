package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProduct(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 80, message = "Name must not exceed 80 characters")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        BigDecimal price,

        @PositiveOrZero(message = "Stock must be 0 or greater")
        int stock
) {}