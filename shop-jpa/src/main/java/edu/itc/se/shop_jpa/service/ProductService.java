package edu.itc.se.shop_jpa.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.web.dto.CreateProduct;
import edu.itc.se.shop_jpa.web.dto.ProductDto;

@Service
public class ProductService {

    // 1. ADD LOGGER HERE:
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> search(String q) {
        List<Product> all = products.findAll();
        if (q == null || q.isBlank()) {
            return all.stream().map(ProductDto::from).toList();
        }
        String lowerQuery = q.toLowerCase();
        return all.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery))
                .map(ProductDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto findById(long id) {
        return products.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public ProductDto create(CreateProduct cmd) {
        String sku = "SKU-" + System.currentTimeMillis();
        Product product = new Product(
                sku,
                cmd.name(),
                Money.eur(cmd.price().toString()),
                Category.HARDWARE,
                cmd.stock()
        );
        Product saved = products.save(product);

        // 2. ADD LOG STATEMENT HERE:
        log.info("created product {} '{}'", saved.getId(), saved.getName());

        return ProductDto.from(saved);
    }

    @Transactional
    public ProductDto updateStock(long id, int delta) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        int newStock = product.getStock() + delta;
        if (newStock < 0) {
            throw new InsufficientStockException(id, product.getStock(), Math.abs(delta));
        }

        product.setStock(newStock);
        return ProductDto.from(product);
    }

    @Transactional
    public void delete(long id) {
        if (!products.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        products.deleteById(id);
    }
}