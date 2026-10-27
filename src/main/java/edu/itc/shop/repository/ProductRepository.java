package edu.itc.shop.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import edu.itc.shop.domain.Product;

@Repository
public class ProductRepository {
    private final JdbcClient jdbcClient;

    public ProductRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Product save(String name, BigDecimal price) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcClient.sql("INSERT INTO product(name, price) VALUES (:name, :price)")
                .param("name", name)
                .param("price", price)
                .update(keyHolder, "id");

        Long generatedId = keyHolder.getKey().longValue();
        
        return new Product(generatedId, name, price);
    }

    public Optional<Product> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM product WHERE id = :id")
                .param("id", id)
                .query(Product.class)
                .optional();
    }

    public List<Product> findAll() {
        return jdbcClient.sql("SELECT * FROM product")
                .query(Product.class)
                .list();
    }
}