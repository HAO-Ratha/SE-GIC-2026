package edu.itc.se.shop_jpa.web.dto;

import java.time.Instant;
import java.util.List;
import edu.itc.se.shop_jpa.domain.OrderStatus;

public record OrderDto(Long id, OrderStatus status, Instant placedAt, List<OrderLineDto> lines) { }