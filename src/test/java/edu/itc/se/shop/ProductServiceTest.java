
package edu.itc.se.shop;

import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    ProductService service;

    @Autowired
    ProductRepository repo;

    @Test
    void restockFailsWhenSourceHasTooLittleStock() {
        int before1 = repo.findById(1).orElseThrow().stock();
        int before2 = repo.findById(2).orElseThrow().stock();

        assertThrows(
                IllegalStateException.class,
                () -> service.restock(1, 2, 1000)
        );

        assertEquals(
                before1, repo.findById(1).orElseThrow().stock()
        );
        assertEquals(
                before2, repo.findById(2).orElseThrow().stock()
        );
    }

    @Test
    void restockRollsBackIfDestinationDoesNotExist() {
        int before1 = repo.findById(1).orElseThrow().stock();

        assertThrows(
                IllegalStateException.class,
                () -> service.restock(1, 999, 1)
        );

        assertEquals(
                before1, repo.findById(1).orElseThrow().stock()
        );
    }
}
