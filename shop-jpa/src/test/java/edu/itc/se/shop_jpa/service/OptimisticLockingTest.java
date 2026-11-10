package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OptimisticLockingTest {

    @Autowired
    OrderRepository orders;

    @Autowired
    TransactionTemplate tx;

    @Test
    void secondWriterFailsWithOptimisticLock() throws Exception {
        Long orderId = createOrder();

        CountDownLatch bothLoaded = new CountDownLatch(2);

        Function<OrderStatus, Callable<Throwable>> writer = target -> () -> {
            try {
                tx.executeWithoutResult(status -> {
                    Order o = orders.findById(orderId).orElseThrow();
                    bothLoaded.countDown();
                    await(bothLoaded);
                    o.setStatus(target);
                });
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Throwable>> results = pool.invokeAll(
                    List.of(writer.apply(OrderStatus.PAID),
                            writer.apply(OrderStatus.CANCELLED)));

            List<Throwable> failures = results.stream()
                    .map(this::get)
                    .filter(Objects::nonNull)
                    .toList();

            assertThat(failures).hasSize(1);
            assertThat(failures.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);

            Order after = orders.findById(orderId).orElseThrow();
            assertThat(after.getStatus()).isIn(OrderStatus.PAID, OrderStatus.CANCELLED);
        }
    }

    private Long createOrder() {
        // create a customer and one order, return id
        return 1L;
    }

    private static void await(CountDownLatch l) {
        try {
            if (!l.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Throwable get(Future<Throwable> f) {
        try {
            return f.get();
        } catch (Exception e) {
            return e;
        }
    }
}