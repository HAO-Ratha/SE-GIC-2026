package edu.itc.shop.web;

import edu.itc.shop.domain.Product;
import edu.itc.shop.service.ProductService;
import edu.itc.shop.web.dto.CreateProduct;
import edu.itc.shop.web.dto.productDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/products
    // GET /api/products?q=laptop
    @GetMapping
    public List<productDto> getAll(
            @RequestParam(required = false) String q
    ) {
        return productService.findAll(q)
                .stream()
                .map(productDto::from)
                .toList();
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public productDto getById(@PathVariable long id) {

        return productDto.from(
                productService.findById(id)
        );
    }

    // POST /api/products
    @PostMapping
    public ResponseEntity<productDto> create(
            @Valid @RequestBody CreateProduct request
    ) {

        Product product = productService.create(
                request.name(),
                request.price(),
                request.stock()
        );

        productDto response = productDto.from(product);

        return ResponseEntity
                .created(URI.create("/api/products/" + product.id()))
                .body(response);
    }

    // PATCH /api/products/{id}/stock
    @PatchMapping("/{id}/stock")
    public productDto updateStock(
            @PathVariable long id,
            @Valid @RequestBody StockChange request
    ) {

        return productDto.from(
                productService.updateStock(id, request.delta())
        );
    }

    // DELETE /api/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {

        productService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // Request body for PATCH
    public record StockChange(
            @NotNull Integer delta
    ) {
    }
}