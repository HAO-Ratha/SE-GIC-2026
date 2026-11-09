package edu.itc.se.shop_jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;
import jakarta.persistence.EntityManager;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired EntityManager em;

    Customer ada;

    @BeforeEach
    void setUp() {
        ada = customers.save(new Customer("Ada Lovelace", "ada@itc.edu"));
        Product kb = products.save(new Product("KB-1", "Keyboard",
                new Money(new BigDecimal("40")), 2, Category.HARDWARE));

        Order o1 = new Order(ada, OrderStatus.PAID, Instant.parse("2026-09-01T10:00:00Z"));
        o1.addLine(kb, 1);
        Order o2 = new Order(ada, OrderStatus.PAID, Instant.parse("2026-09-02T10:00:00Z"));
        o2.addLine(kb, 2);
        orders.saveAll(List.of(o1, o2));

        em.flush();
        em.clear();
    }

    @Test
    void derivedQuery_returnsNewestFirst() {
        var result = orders.findByCustomerEmailAndStatusOrderByPlacedAtDesc(
                "ada@itc.edu", OrderStatus.PAID);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPlacedAt()).isAfter(result.get(1).getPlacedAt());
    }

    @Test
    void fetchJoin_initializesLines_plainFindByIdDoesNot() {
        var fetched = orders.findWithLinesByCustomerEmail("ada@itc.edu");
        assertThat(fetched).hasSize(2);   // distinct removes duplicates
        assertThat(Hibernate.isInitialized(fetched.get(0).getLines())).isTrue();

        Long id = fetched.get(0).getId();
        em.clear();
        Order plain = orders.findById(id).orElseThrow();
        assertThat(Hibernate.isInitialized(plain.getLines())).isFalse();
    }

    @Test
    void nativeQuery_findLowStock() {
        assertThat(products.findLowStock(5)).extracting(Product::getSku).containsExactly("KB-1");
        assertThat(products.findLowStock(1)).isEmpty();
    }

    @Test
    void projection_exposesNestedCustomerName() {
        Page<OrderSummary> page = orders.findByStatus(OrderStatus.PAID,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "placedAt")));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCustomer().getName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void aggregates_countByStatus_and_countLinesByStatus() {
        assertThat(orders.countByStatus(OrderStatus.PAID)).isEqualTo(2);
        assertThat(orders.countLinesByStatus(OrderStatus.PAID)).isEqualTo(2);
    }
}