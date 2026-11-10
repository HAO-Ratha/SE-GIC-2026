package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DetachedEntityTest {

    @Autowired
    ProductRepository products;

    @Test
    void detachedEntityDoesNotPersistUntilMerged() {
        Product p = new Product("SKU-99", "Widget",
                new Money(new BigDecimal("10.00"), "EUR"), Category.HARDWARE, 5);
        Product saved = products.saveAndFlush(p);

        Long id = saved.getId();

        Product detached = products.findById(id).orElseThrow();
        detached.setStock(99);

        Product reloadedBeforeMerge = products.findById(id).orElseThrow();
        assertThat(reloadedBeforeMerge.getStock()).isNotEqualTo(99);

        products.save(detached);

        Product afterMerge = products.findById(id).orElseThrow();
        assertThat(afterMerge.getStock()).isEqualTo(99);
    }
}