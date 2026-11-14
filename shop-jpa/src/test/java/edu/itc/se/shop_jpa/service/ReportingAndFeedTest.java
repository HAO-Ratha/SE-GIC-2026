package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import edu.itc.se.shop_jpa.repository.TagRepository;
import edu.itc.se.shop_jpa.web.dto.OrderFeedResponse;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;
import edu.itc.se.shop_jpa.web.ReportController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
class ReportingAndFeedTest {

    @Autowired
    ProductRepository products;

    @Autowired
    TagRepository tags;

    @Autowired
    CustomerRepository customers;

    @Autowired
    OrderRepository orders;

    @Autowired
    OrderFeedService feed;

    @Autowired
    ReportController reportController;

    @Test
    void criteriaReportCombinesFiltersAndCountsTags() {
        String suffix = UUID.randomUUID().toString();
        Tag sale = tags.saveAndFlush(new Tag("sale-" + suffix));
        Product matched = new Product(
                "RPT-" + suffix.substring(0, 10),
                "Report keyboard",
                new Money(new BigDecimal("129.00"), "EUR"),
                Category.HARDWARE,
                4);
        matched.getTags().add(sale);
        products.saveAndFlush(matched);

        List<ProductReportRow> rows = products.findProductReport(
                "keyboard", Category.HARDWARE, new BigDecimal("150.00"), 1,
                sale.getName(), "amount,desc");

        assertThat(rows).contains(new ProductReportRow(
                matched.getSku(), matched.getName(), new BigDecimal("129.00"), 4, 1));
        assertThat(rows).extracting(ProductReportRow::tagCount).contains(1L);
        assertThat(products.findProductReport(
                null, null, null, null, null, "stock,asc"))
                .contains(new ProductReportRow(
                        matched.getSku(), matched.getName(), new BigDecimal("129.00"), 4, 1));
        assertThatThrownBy(() -> products.findProductReport(
                null, null, null, null, null, "id,desc"))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
        assertThatThrownBy(() -> reportController.products(
                null, null, null, null, null, "id,desc"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void sliceAndKeysetFeedsUseCursorsWithoutDuplicates() {
        String email = "feed-" + UUID.randomUUID() + "@itc.edu";
        Customer customer = customers.saveAndFlush(new Customer(
                "Feed customer", email,
                new Address("Street", "City", "1"),
                new Address("Street", "City", "1")));
        Instant base = Instant.parse("2026-09-20T08:00:00Z");
        List<Order> seeded = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            seeded.add(new Order(customer, OrderStatus.NEW, base.plusSeconds(index)));
        }
        orders.saveAllAndFlush(seeded);

        assertFeedWalk(feed::slice);
        assertFeedWalk(feed::keyset);
    }

    private static void assertFeedWalk(FeedCall call) {
        String cursor = null;
        List<Long> found = new ArrayList<>();
        boolean hasNext;
        do {
            OrderFeedResponse response = call.next(cursor, 2);
            response.items().forEach(item -> found.add(item.id()));
            cursor = response.nextCursor();
            hasNext = response.hasNext();
        } while (hasNext);

        assertThat(found).hasSizeGreaterThanOrEqualTo(5);
        assertThat(found).doesNotHaveDuplicates();
    }

    @FunctionalInterface
    private interface FeedCall {
        OrderFeedResponse next(String after, int size);
    }
}
