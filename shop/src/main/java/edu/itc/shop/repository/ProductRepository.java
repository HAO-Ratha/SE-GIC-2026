
package edu.itc.shop.repository;

import edu.itc.shop.domain.Product;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // 1. Get every product
    public List<Product> findAll() {
        return jdbc.sql(
                "SELECT id, name, price, stock FROM product ORDER BY id"
        ).query(Product.class).list();
    }

    // 2. Find one product by ID
    public Optional<Product> findById(long id) {
        return jdbc.sql(
                "SELECT id, name, price, stock FROM product WHERE id = :id"
        )
        .param("id", id)
        .query(Product.class)
        .optional();
    }

    // 3. Search product names
    public List<Product> findByNameContaining(String name) {
        return jdbc.sql(
                "SELECT id, name, price, stock FROM product " +
                "WHERE LOWER(name) LIKE LOWER(:name) ORDER BY id"
        )
        .param("name", "%" + name + "%")
        .query(Product.class)
        .list();
    }

    // 4. Insert a product and return its generated ID
    public long insert(Product p) {
        var keys = new GeneratedKeyHolder();

        jdbc.sql(
                "INSERT INTO product(name, price, stock) " +
                "VALUES (:name, :price, :stock)"
        )
        .param("name", p.name())
        .param("price", p.price())
        .param("stock", p.stock())
        .update(keys, "id");

        Number key = keys.getKey();

        if (key == null) {
            throw new IllegalStateException("No generated ID returned");
        }

        return key.longValue();
    }

    // 5. Increase or decrease stock
    public int updateStock(long id, int delta) {
        return jdbc.sql(
                "UPDATE product SET stock = stock + :delta " +
                "WHERE id = :id AND stock + :delta >= 0"
        )
        .param("delta", delta)
        .param("id", id)
        .update();
    }

    // 6. Delete a product
    public int delete(long id) {
        return jdbc.sql(
                "DELETE FROM product WHERE id = :id"
        )
        .param("id", id)
        .update();
    }
}
