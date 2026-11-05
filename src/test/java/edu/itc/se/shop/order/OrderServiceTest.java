package edu.itc.se.shop.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.itc.se.shop.common.BadRequestException;
import edu.itc.se.shop.common.ConflictException;
import edu.itc.se.shop.config.ShopProperties;
import edu.itc.se.shop.customer.CustomerRepository;
import edu.itc.se.shop.service.Notifier;
import edu.itc.se.shop.product.Product;
import edu.itc.se.shop.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Service logic with mocked repositories (no Spring, no database). */
class OrderServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final Notifier notifier = mock(Notifier.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private final ShopProperties props = new ShopProperties(new BigDecimal("0.20"));

    private final OrderService service =
            new OrderService(orders, products, customers, props, notifier, clock);

    private static PlaceOrder cmd(PlaceOrder.Line... lines) {
        return new PlaceOrder(1L, List.of(lines));
    }

    @Test
    void placeSnapshotsPricesAndComputesTotal() {
        when(customers.exists(1L)).thenReturn(true);
        when(products.findById(1L)).thenReturn(Optional.of(new Product(1, "Keyboard", new BigDecimal("49.90"), 20)));
        when(products.findById(3L)).thenReturn(Optional.of(new Product(3, "Cable", new BigDecimal("10.00"), 50)));
        when(products.updateStock(anyLong(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(1);
        when(orders.insert(any(Order.class), anyList())).thenReturn(17L);

        OrderDto dto = service.place(cmd(new PlaceOrder.Line(1L, 2), new PlaceOrder.Line(3L, 1)));

        assertEquals(17L, dto.id());
        assertEquals(OrderStatus.NEW, dto.status());
        assertEquals(new BigDecimal("131.76"), dto.total());
        assertEquals(2, dto.lines().size());
        verify(products).updateStock(1L, -2);
        verify(products).updateStock(3L, -1);
    }

    @Test
    void duplicateProductIsRejectedBeforeAnythingIsTouched() {
        assertThrows(BadRequestException.class,
                () -> service.place(cmd(new PlaceOrder.Line(1L, 1), new PlaceOrder.Line(1L, 2))));
        verifyNoInteractions(products, orders);
    }

    @Test
    void insufficientStockThrowsAndNothingIsInserted() {
        when(customers.exists(1L)).thenReturn(true);
        when(products.findById(2L)).thenReturn(Optional.of(new Product(2, "Mouse", new BigDecimal("25.00"), 5)));
        when(products.updateStock(2L, -999)).thenReturn(0);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> service.place(cmd(new PlaceOrder.Line(2L, 999))));

        assertEquals("Insufficient stock for product 2", ex.getMessage());
        verify(orders, never()).insert(any(), anyList());
    }

    @Test
    void cancelRestoresStockAndNotifies() {
        Order order = new Order(5L, 1L, OrderStatus.NEW, LocalDateTime.of(2026, 1, 1, 10, 0), new BigDecimal("131.76"));
        when(orders.findById(5L)).thenReturn(Optional.of(order));
        when(orders.updateStatus(5L, OrderStatus.NEW, OrderStatus.CANCELLED)).thenReturn(1);
        when(orders.findLines(5L)).thenReturn(List.of(
                new OrderLine(1, 2, new BigDecimal("49.90")),
                new OrderLine(3, 1, new BigDecimal("10.00"))));

        OrderDto dto = service.cancel(5L);

        assertEquals(OrderStatus.CANCELLED, dto.status());
        verify(products).updateStock(1L, 2);
        verify(products).updateStock(3L, 1);
        verify(notifier).send(anyString(), contains("5"));
    }

    @Test
    void cancelOfNonNewOrderIsAConflict() {
        Order paid = new Order(6L, 1L, OrderStatus.PAID, LocalDateTime.now(clock), new BigDecimal("10.00"));
        when(orders.findById(6L)).thenReturn(Optional.of(paid));

        assertThrows(ConflictException.class, () -> service.cancel(6L));

        verify(orders, never()).updateStatus(anyLong(), any(), any());
        verifyNoInteractions(products, notifier);
    }
}