package edu.itc.se.shop.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.se.shop.common.Page;
import edu.itc.se.shop.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Level 4: real server on a random port, real H2 database, all layers (controller, service,
 * transaction, repositories, audit aspect). Uses RestClient so it does not depend on the exact
 * test-client package layout of the Spring Boot version.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderEndToEndTest {

    @Value("${local.server.port}")
    int port;

    RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    private int stock(long productId) {
        return client.get().uri("/api/products/{id}", productId).retrieve().body(Product.class).stock();
    }

    private ResponseEntity<OrderDto> place(PlaceOrder cmd) {
        return client.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .body(cmd).retrieve().toEntity(OrderDto.class);
    }

    @Test
    void placeCheckStockCancelCheckStockAgain() {
        int keyboard = stock(1);
        int cable = stock(3);

        // 1. place an order -> 201 + Location + totals
        ResponseEntity<OrderDto> created = place(new PlaceOrder(1L,
                List.of(new PlaceOrder.Line(1L, 2), new PlaceOrder.Line(3L, 1))));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        OrderDto order = created.getBody();
        assertNotNull(order);
        assertEquals("/api/orders/" + order.id(), created.getHeaders().getLocation().getPath());
        assertEquals(OrderStatus.NEW, order.status());
        assertEquals(new BigDecimal("131.76"), order.total());            // (2 x 49.90 + 10.00) x 1.20

        // 2. stock was reserved
        assertEquals(keyboard - 2, stock(1));
        assertEquals(cable - 1, stock(3));

        // 3. the order can be read back with its lines
        OrderDto loaded = client.get().uri("/api/orders/{id}", order.id()).retrieve().body(OrderDto.class);
        assertEquals(2, loaded.lines().size());

        // 4. cancel -> stock is back
        OrderDto cancelled = client.post().uri("/api/orders/{id}/cancel", order.id()).retrieve().body(OrderDto.class);
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(keyboard, stock(1));
        assertEquals(cable, stock(3));

        // 5. a second cancel is a conflict and must not restore stock twice
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> client.post().uri("/api/orders/{id}/cancel", order.id()).retrieve().body(OrderDto.class));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(keyboard, stock(1));
    }

    @Test
    void insufficientStockReturns409AndRollsBackTheEarlierLines() {
        int keyboard = stock(1);
        int mouse = stock(2);

        // line 1 is fine and is decremented first; line 2 asks for far too much
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                place(new PlaceOrder(1L, List.of(new PlaceOrder.Line(1L, 2), new PlaceOrder.Line(2L, 999)))));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Insufficient stock for product 2"));
        assertEquals(keyboard, stock(1));   // the first decrement was rolled back
        assertEquals(mouse, stock(2));
    }

    @Test
    void validationAndDuplicateErrorsAre400() {
        HttpClientErrorException empty = assertThrows(HttpClientErrorException.class,
                () -> place(new PlaceOrder(1L, List.of())));
        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatusCode());

        HttpClientErrorException dup = assertThrows(HttpClientErrorException.class,
                () -> place(new PlaceOrder(1L, List.of(new PlaceOrder.Line(1L, 1), new PlaceOrder.Line(1L, 1)))));
        assertEquals(HttpStatus.BAD_REQUEST, dup.getStatusCode());
    }

    @Test
    void listIsPaginatedAndFiltered() {
        for (int i = 0; i < 3; i++) {
            place(new PlaceOrder(2L, List.of(new PlaceOrder.Line(3L, 1))));
        }

        Page<OrderDto> first = client.get()
                .uri("/api/orders?customerId=2&status=NEW&page=0&size=2")
                .retrieve().body(new ParameterizedTypeReference<>() {});

        assertEquals(2, first.content().size());
        assertEquals(0, first.page());
        assertEquals(2, first.size());
        assertTrue(first.totalElements() >= 3);
        assertTrue(first.content().stream().allMatch(o -> o.status() == OrderStatus.NEW));
    }
}