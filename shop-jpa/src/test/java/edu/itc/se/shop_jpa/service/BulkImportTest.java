package edu.itc.se.shop_jpa.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.itc.se.shop_jpa.repository.ProductRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.engine.jdbc.batch=OFF"
})
class BulkImportTest {

    private static final Logger log = LoggerFactory.getLogger(BulkImportTest.class);
    private static final int PRODUCT_COUNT = 50_000;

    @TempDir
    Path tempDir;

    @Autowired
    BulkImportService importer;

    @Autowired
    ProductRepository products;

    @Test
    void importsFiftyThousandProductsUsingThreeStrategies() throws IOException {
        Measurement stateless = measure("stateless", "SS-", csv("SS-"));
        Measurement saveAll = measure("saveAll", "SA-", csv("SA-"), false);
        Measurement saveAllCleared = measure("saveAll+flush/clear", "SC-", csv("SC-"), true);

        assertThat(stateless.imported()).isEqualTo(PRODUCT_COUNT);
        assertThat(saveAll.imported()).isEqualTo(PRODUCT_COUNT);
        assertThat(saveAllCleared.imported()).isEqualTo(PRODUCT_COUNT);
        assertThat(stateless.elapsedMillis()).isLessThan(10_000);
    }

    private Measurement measure(String name, String prefix, Path csv) throws IOException {
        return measure(name, prefix, csv, null);
    }

    private Measurement measure(String name, String prefix, Path csv, Boolean clearEveryChunk)
            throws IOException {
        long heapBefore = usedHeap();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong peakHeap = new AtomicLong(heapBefore);
        Thread sampler = Thread.ofPlatform().daemon().start(() -> {
            while (running.get()) {
                peakHeap.accumulateAndGet(usedHeap(), Math::max);
                LockSupportSupport.pause();
            }
        });
        long started = System.nanoTime();
        int imported;
        try {
            imported = clearEveryChunk == null
                    ? importer.importCsv(csv)
                    : importer.importWithSaveAll(csv, clearEveryChunk);
        } finally {
            running.set(false);
            try {
                sampler.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while measuring import heap", ex);
            }
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        long heapDeltaMiB = Math.max(0, peakHeap.get() - heapBefore) / (1024 * 1024);
        log.info("{}: {} rows in {} ms, peak heap delta {} MiB",
                name, imported, elapsedMillis, heapDeltaMiB);
        assertThat(products.existsBySku(prefix + "0")).isTrue();
        return new Measurement(imported, elapsedMillis, heapDeltaMiB);
    }

    private Path csv(String prefix) throws IOException {
        Path path = tempDir.resolve(prefix + UUID.randomUUID() + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("sku;name;amount;currency;category;stock");
            writer.newLine();
            for (int index = 0; index < PRODUCT_COUNT; index++) {
                writer.write(prefix + index + ";Supplier product " + index + ";12.50;EUR;HARDWARE;10");
                writer.newLine();
            }
        }
        return path;
    }

    private static long usedHeap() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private record Measurement(int imported, long elapsedMillis, long heapDeltaMiB) {
    }

    private static final class LockSupportSupport {
        private LockSupportSupport() {
        }

        static void pause() {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
