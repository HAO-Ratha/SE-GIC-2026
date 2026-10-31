
package edu.itc.shop.repository;

import edu.itc.shop.domain.Product;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RawJdbcProductRepository {

    private final DataSource dataSource;

    public RawJdbcProductRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Product> findAll() {
        String sql =
                "SELECT id, name, price, stock FROM product ORDER BY id";

        List<Product> products = new ArrayList<>();

        try (var connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Product product = new Product(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getInt("stock")
                );

                products.add(product);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to read products", e);
        }

        return products;
    }
}

