package edu.itc.shop.service;

import edu.itc.shop.domain.Product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Statement;
import java.util.List;

@Service
public class ProductService {

    private final JdbcTemplate jdbc;

    public ProductService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Product> rowMapper = (rs, rowNum) ->
            new Product(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price"),
                    rs.getInt("stock")
            );

    // GET: Find all products or search by name
    public List<Product> findAll(String q) {

        if (q == null || q.isBlank()) {
            return jdbc.query(
                    "SELECT * FROM products ORDER BY id",
                    rowMapper
            );
        }

        return jdbc.query(
                "SELECT * FROM products WHERE LOWER(name) LIKE ? ORDER BY id",
                rowMapper,
                "%" + q.toLowerCase() + "%"
        );
    }

    // GET: Find product by ID
    public Product findById(long id) {

        return jdbc.query(
                "SELECT * FROM products WHERE id = ?",
                rowMapper,
                id
        ).stream()
         .findFirst()
         .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // POST: Create product
    public Product create(
            String name,
            java.math.BigDecimal price,
            int stock
    ) {

        String sql = """
                INSERT INTO products(name, price, stock)
                VALUES (?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, name);
            statement.setBigDecimal(2, price);
            statement.setInt(3, stock);

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException("Failed to generate product ID");
        }

        return findById(key.longValue());
    }

    // PATCH: Update product stock
    public Product updateStock(long id, int delta) {

        // Check whether product exists
        findById(id);

        int updated = jdbc.update(
                """
                UPDATE products
                SET stock = stock + ?
                WHERE id = ? AND stock + ? >= 0
                """,
                delta, id, delta
        );

        if (updated == 0) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + id
            );
        }

        return findById(id);
    }

    // DELETE: Delete product
    public void delete(long id) {

        int deleted = jdbc.update(
                "DELETE FROM products WHERE id = ?",
                id
        );

        if (deleted == 0) {
            throw new ProductNotFoundException(id);
        }
    }
}