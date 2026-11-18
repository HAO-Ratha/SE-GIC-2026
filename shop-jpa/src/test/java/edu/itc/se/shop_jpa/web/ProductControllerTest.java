package edu.itc.se.shop_jpa.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.itc.se.shop_jpa.service.ProductNotFoundException;
import edu.itc.se.shop_jpa.service.ProductService;
import edu.itc.se.shop_jpa.web.dto.ProductDto;

@WebMvcTest(ProductController.class)
@Import(ApiExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService service;

    @Test
    void listProductsReturns200() throws Exception {
        when(service.search(null)).thenReturn(List.of(
                new ProductDto(1L, "USB Cable", new BigDecimal("9.99"), 50)
        ));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("USB Cable"));
    }

    @Test
    void productNotFoundReturns404ProblemDetail() throws Exception {
        when(service.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        mvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product 999 not found"));
    }

    @Test
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "price": 0, "stock": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void createProductReturns201WithLocationHeader() throws Exception {
        ProductDto created = new ProductDto(42L, "Keyboard", new BigDecimal("49.99"), 10);
        when(service.create(any())).thenReturn(created);

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keyboard", "price": 49.99, "stock": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }
}