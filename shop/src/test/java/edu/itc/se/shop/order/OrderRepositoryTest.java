package edu.itc.se.shop.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@ActiveProfiles("test")
@Import(OrderRepository.class)
class OrderRepositoryTest {

    @Autowired
    OrderRepository orders;

    private long insert(long customerId, OrderStatus status) {
        List<OrderLine> lines = List.of(
                new OrderLine(1, 2, new BigDecimal("49.90")),
                new OrderLine(3, 1, new BigDecimal("10.00")));
        Order o = new Order(null, customerId, status, LocalDateTime.of(2026, 1, 1, 10, 0), new BigDecimal("131.76"));
        return orders.insert(o, lines);
    }

    @Test
    void insertThenFindByIdWithLines() {
        long id = insert(1, OrderStatus.NEW);

        Order o = orders.findById(id).orElseThrow();
        assertEquals(1L, o.customerId());
        assertEquals(OrderStatus.NEW, o.status());
        assertEquals(new BigDecimal("131.76"), o.total());

        List<OrderLine> lines = orders.findLines(id);
        assertEquals(2, lines.size());
        assertEquals(1L, lines.get(0).productId());
        assertEquals(new BigDecimal("49.90"), lines.get(0).unitPrice());
    }

    @Test
    void paginationUsesLimitAndOffsetAndCountsTheTotal() {
        for (int i = 0; i < 5; i++) insert(1, OrderStatus.NEW);

        assertEquals(5, orders.count(1L, null));
        assertEquals(2, orders.find(1L, null, 2, 0).size());
        assertEquals(2, orders.find(1L, null, 2, 2).size());
        assertEquals(1, orders.find(1L, null, 2, 4).size());      // last, partial page
        assertTrue(orders.find(1L, null, 2, 10).isEmpty());       // past the end
    }

    @Test
    void filtersByCustomerAndStatus() {
        insert(1, OrderStatus.NEW);
        insert(1, OrderStatus.CANCELLED);
        insert(2, OrderStatus.NEW);

        assertEquals(2, orders.count(1L, null));
        assertEquals(1, orders.count(1L, OrderStatus.CANCELLED));
        assertEquals(1, orders.count(2L, OrderStatus.NEW));
        assertEquals(3, orders.count(null, null));
        assertEquals(2, orders.count(null, OrderStatus.NEW));
    }

    @Test
    void updateStatusOnlyWorksFromTheExpectedState() {
        long id = insert(1, OrderStatus.NEW);

        assertEquals(1, orders.updateStatus(id, OrderStatus.NEW, OrderStatus.CANCELLED));
        assertEquals(0, orders.updateStatus(id, OrderStatus.NEW, OrderStatus.CANCELLED)); // already cancelled
        assertEquals(OrderStatus.CANCELLED, orders.findById(id).orElseThrow().status());
    }

    @Test
    void findLinesByOrderIdsGroupsPerOrder() {
        long a = insert(1, OrderStatus.NEW);
        long b = insert(1, OrderStatus.NEW);

        var grouped = orders.findLinesByOrderIds(List.of(a, b));
        assertEquals(2, grouped.get(a).size());
        assertEquals(2, grouped.get(b).size());
        assertTrue(orders.findLinesByOrderIds(List.of()).isEmpty());
    }
}