package edu.itc.shop.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Product;
import edu.itc.shop.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void createTwoProductsWithTransaction(String name1, String name2) {
        repository.save(name1, new BigDecimal("10.00"));
        System.out.println("Saved first product: " + name1);
        
        // Simulate an error to test rollback
        if (name2 == null || name2.isEmpty()) {
            throw new IllegalArgumentException("Second product name is empty! Rolling back transaction.");
        }
        
        repository.save(name2, new BigDecimal("20.00"));
        System.out.println("Saved second product: " + name2);
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    // THIS was the missing piece or the broken part!
    public Product createProduct(String name, BigDecimal price) {
        return repository.save(name, price);
    }
}