package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.engine.jdbc.batch=OFF"
})
class StockReservationTest {

    private static final Logger log = LoggerFactory.getLogger(StockReservationTest.class);
    private static final int THREADS = 10;

    @Autowired
    ProductRepository products;

    @Autowired
    StockService stock;

    @Test
    void pessimisticReservationNeverOversells() throws Exception {
        verifyConcurrentReservations("pessimistic", stock::reserve);
    }

    @Test
    void optimisticReservationRetriesAndNeverOversells() throws Exception {
        stock.resetOptimisticRetryCount();
        verifyConcurrentReservations("optimistic", stock::reserveOptimistically);
        log.info("Optimistic retries: {}", stock.optimisticRetryCount());
    }

    @Test
    void atomicUpdateNeverOversells() throws Exception {
        verifyConcurrentReservations("atomic update", stock::reserveAtomically);
    }

    private void verifyConcurrentReservations(String variant, BiConsumer<Long, Integer> reserve)
            throws Exception {
        Product product = products.saveAndFlush(new Product(
                "R" + UUID.randomUUID().toString().substring(0, 12),
                "Reservation test",
                new Money(new BigDecimal("1.00"), "EUR"),
                Category.HARDWARE,
                5));

        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        long startedAt = System.nanoTime();
        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Reservation start timed out");
                    }
                    try {
                        reserve.accept(product.getId(), 1);
                        return true;
                    } catch (OutOfStockException ex) {
                        return false;
                    }
                }));
            }
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Reservation workers did not become ready");
            }
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    successes++;
                }
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            int finalStock = products.findById(product.getId()).orElseThrow().getStock();
            assertThat(successes).isEqualTo(5);
            assertThat(finalStock).isZero();
            double throughput = elapsedMillis == 0 ? Double.POSITIVE_INFINITY : THREADS * 1000.0 / elapsedMillis;
            log.info("{} reservation: {} ok / {} out-of-stock, stock={}, {} threads in {} ms ({} ops/s)",
                    variant, successes, THREADS - successes, finalStock, THREADS, elapsedMillis, throughput);
        }
    }
}
