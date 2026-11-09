package edu.itc.se.shop_jpa.config;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "shop")
@Validated
public record ShopProperties(
        @NotBlank String currency,
        @NotNull @DecimalMin("0.0") BigDecimal taxRate,
        @PositiveOrZero int lowStockThreshold
) {}