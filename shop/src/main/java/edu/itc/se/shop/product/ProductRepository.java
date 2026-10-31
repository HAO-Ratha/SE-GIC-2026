package edu.itc.se.shop.product;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private static final RowMapper<Product> MAPPER = (rs, i) ->
            new Product(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("stock"));

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAll() {
        return jdbc.sql("SELECT id, name, price, stock FROM product ORDER BY id").query(MAPPER).list();
    }

    public Optional<Product> findById(long id) {
        return jdbc.sql("SELECT id, name, price, stock FROM product WHERE id = :id")
                .param("id", id).query(MAPPER).optional();
    }

    /**
     * Atomically adds {@code delta} to the stock (negative = reserve). The WHERE clause refuses to
     * go below zero, so two concurrent orders can never oversell. Returns the number of rows changed:
     * 0 means "product missing or not enough stock".
     */
    public int updateStock(long productId, int delta) {
        return jdbc.sql("UPDATE product SET stock = stock + :delta WHERE id = :id AND stock + :delta >= 0")
                .param("delta", delta)
                .param("id", productId)
                .update();
    }
}