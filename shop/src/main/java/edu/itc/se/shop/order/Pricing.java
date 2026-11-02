package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Pure pricing rules: no Spring, no database, trivial to unit test. */
public final class Pricing {

    private Pricing() {}

    /** total = net * (1 + taxRate), rounded HALF_UP to 2 decimals. */
    public static BigDecimal total(List<OrderLine> lines, BigDecimal taxRate) {
        BigDecimal net = lines.stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.qty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return net.multiply(BigDecimal.ONE.add(taxRate)).setScale(2, RoundingMode.HALF_UP);
    }
}