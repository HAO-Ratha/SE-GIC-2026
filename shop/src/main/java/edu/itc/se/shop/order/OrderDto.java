package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderDto(long id, OrderStatus status, BigDecimal total, List<LineDto> lines) {

    public record LineDto(long productId, int qty, BigDecimal unitPrice) {}

    static OrderDto from(Order order, List<OrderLine> lines) {
        return new OrderDto(order.id(), order.status(), order.total(),
                lines.stream().map(l -> new LineDto(l.productId(), l.qty(), l.unitPrice())).toList());
    }

    /** Compact form used in the audit log: "order 17". */
    @Override
    public String toString() {
        return "order " + id;
    }
}