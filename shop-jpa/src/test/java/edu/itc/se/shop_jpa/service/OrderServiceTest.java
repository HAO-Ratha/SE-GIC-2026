package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.repository.OrderRepository;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orders;

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    TestData data;

    @Test
    void managedEntityUpdatesWithoutSave() {

        Long id = data.newOrder();

        Statistics stats = emf
                .unwrap(SessionFactory.class)
                .getStatistics();

        stats.setStatisticsEnabled(true);
        stats.clear();

        orderService.markPaid(id);

        Order result = orders.findById(id)
                .orElseThrow();

        assertThat(result.getStatus())
                .isEqualTo(OrderStatus.PAID);

        assertThat(stats.getEntityUpdateCount())
                .isEqualTo(1);

    }
    void detachedEntityNeedsSave() {

    Long id = data.newProduct();

    // Load product outside a surrounding transaction
    Product p = products.findById(id)
            .orElseThrow();

    // Change Java object
    p.setStock(99);

    // Reload from database
    Product unchanged = products.findById(id)
            .orElseThrow();

    // Database still has original stock
    assertThat(unchanged.getStock())
            .isNotEqualTo(99);

    // Merge detached entity
    products.save(p);

    // Reload again
    Product updated = products.findById(id)
            .orElseThrow();

    assertThat(updated.getStock())
            .isEqualTo(99);
}

