package edu.itc.shop.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProduct(
        @NotBlank(message = "Name cannot be empty")
        String name,

        @NotNull(message = "Price cannot be null")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price
) {}