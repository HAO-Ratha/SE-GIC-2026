package edu.itc.se.shop_jpa.web;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports/products")
public class ReportController {

    private final ProductRepository products;

    public ReportController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<ProductReportRow> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String sort) {
        try {
            return products.findProductReport(q, category, maxPrice, minStock, tag, sort);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (InvalidDataAccessApiUsageException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
