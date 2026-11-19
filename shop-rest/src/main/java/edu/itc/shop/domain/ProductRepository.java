package edu.itc.shop.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<Product> findAll();

    Optional<Product> findById(long id);

    Optional<Product> findBySku(String sku);

    /** Assigns a new id when product.id() == 0. Increments version on every save. */
    Product save(Product product);

    /** Returns true if a product was removed. */
    boolean delete(long id);
}