package edu.itc.se.shop_jpa.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Component;

import edu.itc.se.shop_jpa.domain.Address;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;

@Component
public class TestData {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public TestData(CustomerRepository customers, ProductRepository products, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    public Long newOrder() {
        Address addr = new Address("1 Main St", "London", "N1");
        Customer customer = customers.save(new Customer("Test Customer", "test" + System.nanoTime() + "@itc.edu", addr, addr));
        Order order = new Order(customer, OrderStatus.NEW, Instant.now());
        return orders.save(order).getId();
    }

    public Long newProduct() {
        Product p = new Product("SKU-" + (System.currentTimeMillis() % 1000000), "Test Product", new Money(BigDecimal.TEN, "EUR"), Category.HARDWARE, 10);
        return products.save(p).getId();
    }
}