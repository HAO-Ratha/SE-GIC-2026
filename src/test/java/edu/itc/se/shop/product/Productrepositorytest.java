package edu.itc.se.shop.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Level 2: @JdbcTest runs against embedded H2 with schema.sql + data-test.sql; each test is rolled back. */
@JdbcTest
@ActiveProfiles("test")
@Import(ProductRepository.class)
class ProductRepositoryTest {

    @Autowired
    ProductRepository products;

    @Test
    void findsSeededProduct() {
        Product p = products.findById(1).orElseThrow();
        assertEquals("Mechanical Keyboard", p.name());
        assertEquals(new java.math.BigDecimal("49.90"), p.price());
        assertEquals(20, p.stock());
    }

    @Test
    void updateStockReservesAndRestores() {
        assertEquals(1, products.updateStock(1, -2));
        assertEquals(18, products.findById(1).orElseThrow().stock());
        assertEquals(1, products.updateStock(1, 2));
        assertEquals(20, products.findById(1).orElseThrow().stock());
    }

    @Test
    void updateStockRefusesToGoBelowZero() {
        assertEquals(0, products.updateStock(2, -6));                 // stock is 5
        assertEquals(5, products.findById(2).orElseThrow().stock());  // untouched
    }

    @Test
    void updateStockOnUnknownProductChangesNothing() {
        assertEquals(0, products.updateStock(999, -1));
        assertTrue(products.findById(999).isEmpty());
    }
}