package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderRepository orders;

    @Autowired
    ProductRepository products;

    @Autowired
    CustomerRepository customers;

    @Autowired
    OrderService orderService;

    @Autowired
    EntityManager em;

    @Test
    void markPaid_updatesStatusWithoutSave() {
        Long orderId = createOrder();

        SessionFactory sf = em.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        sf.getStatistics().clear();

        orderService.markPaid(orderId);

        Order reloaded = orders.findById(orderId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);

        long updates = sf.getStatistics().getEntityUpdateCount();
        assertThat(updates).isEqualTo(1);
    }

    private Long createOrder() {
        Customer customer = customers.save(new Customer("Ada Lovelace", "ada-" + UUID.randomUUID() + "@itc.edu"));

        Product product = products.save(new Product(
                "KB-1",
                "Keyboard",
                new Money(new BigDecimal("40.00"), "EUR"),
                Category.HARDWARE,
                5
        ));

        Order order = new Order(customer, OrderStatus.NEW, Instant.now());
        order.addLine(product, 1);

        return orders.save(order).getId();
    }
}