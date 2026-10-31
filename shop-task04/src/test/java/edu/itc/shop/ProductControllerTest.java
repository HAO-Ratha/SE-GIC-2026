package edu.itc.shop;

import edu.itc.shop.domain.Product;
import edu.itc.shop.service.ProductService;
import edu.itc.shop.service.ProductNotFoundException;
import edu.itc.shop.web.ProductController;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    // TEST 1: GET list returns 200
    @Test
    void shouldReturnProductList() throws Exception {

        Product product = new Product(
                1L,
                "Laptop",
                new BigDecimal("899.99"),
                10
        );

        when(productService.findAll(null))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    // TEST 2: GET nonexistent ID returns 404
    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {

        when(productService.findById(99L))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    // TEST 3: POST invalid data returns 400
    @Test
    void shouldReturn400WhenValidationFails() throws Exception {

        String json = """
                {
                    "name": "",
                    "price": null,
                    "stock": 10
                }
                """;

        mockMvc.perform(
                post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    // TEST 4: POST valid data returns 201 and Location
    @Test
    void shouldCreateProduct() throws Exception {

        Product product = new Product(
                1L,
                "Laptop",
                new BigDecimal("899.99"),
                10
        );

        when(productService.create(
                "Laptop",
                new BigDecimal("899.99"),
                10
        )).thenReturn(product);

        String json = """
                {
                    "name": "Laptop",
                    "price": 899.99,
                    "stock": 10
                }
                """;

        mockMvc.perform(
                post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
        )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/products/1"
                ))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }
}