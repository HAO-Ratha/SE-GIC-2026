package edu.itc.se.shop_jpa.service;

import java.util.List;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import edu.itc.se.shop_jpa.config.ShopProperties;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;

@Component
public class LowStockHealthIndicator implements HealthIndicator {

    private final ProductRepository products;
    private final ShopProperties properties;

    public LowStockHealthIndicator(ProductRepository products, ShopProperties properties) {
        this.products = products;
        this.properties = properties;
    }

    @Override
    public Health health() {
        int threshold = properties.lowStockThreshold();
        List<String> lowStockProducts = products.findAll().stream()
                .filter(p -> p.getStock() < threshold)
                .map(Product::getName)
                .toList();

        if (!lowStockProducts.isEmpty()) {
            return Health.down()
                    .withDetail("lowStockProducts", lowStockProducts)
                    .build();
        }
        return Health.up().build();
    }
}