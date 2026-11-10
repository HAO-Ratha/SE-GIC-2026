package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReadOnlyTrapTest {

    @Autowired
    ProductRepository products;

    @Test
    void readOnlyTransactionDoesNotFlushDirtyChanges() {
        Product p = products.saveAndFlush(new Product("SKU-RO", "ReadOnly",
                new Money(new BigDecimal("12.00"), "EUR"), Category.TOYS, 3));

        Product result = wrongReadOnly(p.getId());

        Product reloaded = products.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(3);
        assertThat(result.getStock()).isEqualTo(3);
    }

    @Transactional(readOnly = true)
    Product wrongReadOnly(Long id) {
        Product product = products.findById(id).orElseThrow();
        product.setStock(999); // changed, but not flushed
        return product;
    }
}