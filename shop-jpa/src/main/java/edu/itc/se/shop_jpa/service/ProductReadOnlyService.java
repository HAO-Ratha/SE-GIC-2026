package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReadOnlyService {

    private final ProductRepository products;

    public ProductReadOnlyService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Product wrongReadOnly(Long id) {
        Product product = products.findById(id).orElseThrow();
        product.setStock(999);
        return product;
    }
}
