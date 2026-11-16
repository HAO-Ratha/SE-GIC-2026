package edu.itc.se.shop_jpa.web;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.service.ProductService;
import edu.itc.se.shop_jpa.web.dto.ProductView;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ProductView> list(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Category category,
                                  @RequestParam(required = false) BigDecimal maxPrice,
                                  @RequestParam(required = false) Boolean inStock,
                                  @PageableDefault(size = 20) Pageable pageable) {
        return service.search(q, category, maxPrice, inStock, pageable);
    }
}