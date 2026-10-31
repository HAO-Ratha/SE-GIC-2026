package edu.itc.se.shop.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.se.shop.common.ConflictException;
import edu.itc.se.shop.common.NotFoundException;
import edu.itc.se.shop.common.Page;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Level 3: only the web layer; OrderService is a mock. */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService service;

    private static final String VALID = """
            {"customerId":1,"lines":[{"productId":1,"qty":2},{"productId":3,"qty":1}]}""";

    private static OrderDto sample(long id, OrderStatus status) {
        return new OrderDto(id, status, new BigDecimal("131.76"), List.of(
                new OrderDto.LineDto(1, 2, new BigDecimal("49.90")),
                new OrderDto.LineDto(3, 1, new BigDecimal("10.00"))));
    }

    @Test
    void placeReturns201WithLocationAndBody() throws Exception {
        when(service.place(any(PlaceOrder.class))).thenReturn(sample(17, OrderStatus.NEW));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/17"))
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.total").value(131.76))
                .andExpect(jsonPath("$.lines[0].productId").value(1))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(49.90));
    }

    @Test
    void emptyLinesIsABadRequest() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"lines\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void quantityBelowOneIsABadRequest() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":0}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingCustomerIsABadRequest() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":1,\"qty\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientStockIs409() throws Exception {
        when(service.place(any(PlaceOrder.class))).thenThrow(new InsufficientStockException(2));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Insufficient stock for product 2"));
    }

    @Test
    void getByIdReturnsOrderWithLines() throws Exception {
        when(service.get(17)).thenReturn(sample(17, OrderStatus.NEW));

        mvc.perform(get("/api/orders/17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void getUnknownOrderIs404() throws Exception {
        when(service.get(99)).thenThrow(new NotFoundException("Order 99 not found"));

        mvc.perform(get("/api/orders/99")).andExpect(status().isNotFound());
    }

    @Test
    void listPassesFiltersAndReturnsPageEnvelope() throws Exception {
        when(service.list(eq(1L), eq(OrderStatus.NEW), eq(0), eq(2)))
                .thenReturn(new Page<>(List.of(sample(17, OrderStatus.NEW)), 0, 2, 5));

        mvc.perform(get("/api/orders").param("customerId", "1").param("status", "NEW")
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void invalidStatusFilterIsABadRequest() throws Exception {
        mvc.perform(get("/api/orders").param("status", "BOGUS")).andExpect(status().isBadRequest());
    }

    @Test
    void cancelReturnsCancelledOrder() throws Exception {
        when(service.cancel(17)).thenReturn(sample(17, OrderStatus.CANCELLED));

        mvc.perform(post("/api/orders/17/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOfNonNewOrderIs409() throws Exception {
        when(service.cancel(17)).thenThrow(new ConflictException("Order 17 is CANCELLED; only NEW orders can be cancelled"));

        mvc.perform(post("/api/orders/17/cancel")).andExpect(status().isConflict());
    }
}