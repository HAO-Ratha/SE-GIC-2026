package edu.itc.se.shop.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Level 1: plain JUnit, no Spring. */
class PricingTest {

    @Test
    void addsTaxToTheNetSum() {
        List<OrderLine> lines = List.of(
                new OrderLine(1, 2, new BigDecimal("49.90")),
                new OrderLine(3, 1, new BigDecimal("10.00")));
        // net = 109.80, x 1.20 = 131.76
        assertEquals(new BigDecimal("131.76"), Pricing.total(lines, new BigDecimal("0.20")));
    }

    @Test
    void roundsHalfUp() {
        // 1.35 x 1.10 = 1.4850 -> HALF_UP gives 1.49 (HALF_EVEN would give 1.48)
        List<OrderLine> lines = List.of(new OrderLine(1, 1, new BigDecimal("1.35")));
        assertEquals(new BigDecimal("1.49"), Pricing.total(lines, new BigDecimal("0.10")));
    }

    @Test
    void zeroTaxKeepsTheNetAmount() {
        List<OrderLine> lines = List.of(new OrderLine(1, 3, new BigDecimal("2.50")));
        assertEquals(new BigDecimal("7.50"), Pricing.total(lines, BigDecimal.ZERO));
    }

    @Test
    void noLinesMeansZero() {
        assertEquals(new BigDecimal("0.00"), Pricing.total(List.of(), new BigDecimal("0.20")));
    }
}