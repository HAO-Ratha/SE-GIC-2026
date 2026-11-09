package edu.itc.se.shop_jpa.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop_jpa.domain.Address;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public DataSeeder(CustomerRepository customers, ProductRepository products, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.count() > 0)
            return; // idempotent

        Random rnd = new Random(42);

        List<Customer> cs = customers.saveAll(List.of(
            new Customer("Ada Lovelace", "ada@itc.edu", new Address("1 Main St", "London", "N1"), new Address("1 Main St", "London", "N1")),
            new Customer("Grace Hopper", "grace@itc.edu", new Address("2 Main St", "New York", "10001"), new Address("2 Main St", "New York", "10001")),
            new Customer("Alan Turing", "alan@itc.edu", new Address("3 Main St", "Manchester", "M1"), new Address("3 Main St", "Manchester", "M1"))));

        Tag[] tagPool = { new Tag("wireless"), new Tag("gaming"), new Tag("office") };
        Category[] cats = Category.values();

        List<Product> ps = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Product p = new Product("KB-" + i,
                    (i % 2 == 0 ? "Keyboard " : "Gadget ") + i,
                    new Money(BigDecimal.valueOf(10 + i * 7L), "EUR"),
                    cats[i % cats.length],
                    i % 4 == 0 ? 0 : rnd.nextInt(50));
            p.getTags().add(tagPool[i % 3]);
            if (i % 5 == 0)
                p.getTags().add(tagPool[(i + 1) % 3]);
            ps.add(p);
        }
        products.saveAll(ps); // Tag needs cascade PERSIST (or save tags via a TagRepository first)

        OrderStatus[] statuses = OrderStatus.values();
        for (int i = 0; i < 30; i++) {
            Order o = new Order(cs.get(i % 3),
                    statuses[rnd.nextInt(statuses.length)],
                    Instant.now().minus(i, ChronoUnit.DAYS));
            int lines = 2 + rnd.nextInt(3); // 2-4 lines
            for (int j = 0; j < lines; j++) {
                o.addLine(ps.get(rnd.nextInt(ps.size())), 1 + rnd.nextInt(3));
            }
            orders.save(o);
        }
    }
}