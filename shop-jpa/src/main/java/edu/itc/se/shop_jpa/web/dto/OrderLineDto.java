package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;

public record OrderLineDto(String sku, String productName, int quantity, BigDecimal unitPrice) { }