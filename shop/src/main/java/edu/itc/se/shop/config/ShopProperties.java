package edu.itc.se.shop.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "shop")
public record ShopProperties(@DefaultValue("0.20") BigDecimal taxRate) {}