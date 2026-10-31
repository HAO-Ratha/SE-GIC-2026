package edu.itc.shop.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProduct(

        @NotBlank
        @Size(max = 80)
        String name,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price,

        @PositiveOrZero
        int stock

) {
}