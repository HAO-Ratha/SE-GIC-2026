package edu.itc.se.shop.customer;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbc;

    public CustomerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Customer> findById(long id) {
        return jdbc.sql("SELECT id, name, email FROM customer WHERE id = :id")
                .param("id", id)
                .query((rs, i) -> new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("email")))
                .optional();
    }

    public boolean exists(long id) {
        Long count = jdbc.sql("SELECT COUNT(*) FROM customer WHERE id = :id")
                .param("id", id).query(Long.class).single();
        return count != null && count > 0;
    }
}