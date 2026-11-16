package edu.itc.se.shop_jpa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProductMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EntityManager rawEntityManager;

    @Test
    void testProductMappingAndRoundTrip() {

        Money price = Money.eur("49.99");

        Product product = new Product(
                "SKU-12345",
                "Mechanical Keyboard",
                price,
                Category.HARDWARE,
                10
        );

        Long id = entityManager.persistAndGetId(product, Long.class);

        entityManager.flush();
        entityManager.clear();

        Product reloaded = entityManager.find(Product.class, id);

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getSku()).isEqualTo("SKU-12345");
        assertThat(reloaded.getName()).isEqualTo("Mechanical Keyboard");
        assertThat(reloaded.getCategory()).isEqualTo(Category.HARDWARE);
        assertThat(reloaded.getStock()).isEqualTo(10);

        assertThat(reloaded.getPrice().getAmount())
                .isEqualByComparingTo("49.99");

        assertThat(reloaded.getPrice().getCurrency())
                .isEqualTo("EUR");

        String categoryText = (String) rawEntityManager
                .createNativeQuery(
                        "SELECT category FROM product WHERE id = ?"
                )
                .setParameter(1, id)
                .getSingleResult();

        assertThat(categoryText).isEqualTo("HARDWARE");
    }

    @Test
    void testProductIdIsGenerated() {

        Product product = new Product(
                "SKU-67890",
                "Wireless Mouse",
                Money.eur("25.00"),
                Category.HARDWARE,
                20
        );

        entityManager.persistAndFlush(product);

        assertThat(product.getId()).isNotNull();
        assertThat(product.getId()).isPositive();
    }
}
