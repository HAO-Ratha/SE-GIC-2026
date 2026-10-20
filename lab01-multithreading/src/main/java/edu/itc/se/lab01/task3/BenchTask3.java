package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchTask3 {

    private static final int NUM_PRODUCERS = 2;
    private static final int ORDERS_PER_PRODUCER = 50;
    private static final int NUM_CONSUMERS = 3;
    private static final int BUFFER_CAPACITY = 5;
    private static final long JOIN_TIMEOUT_MS = 10_000;

    /** Set to true to print one line per processed order. */
    static final boolean VERBOSE = false;

    static void runScenario(String label, OrderQueue queue)
            throws InterruptedException {

        Stats stats = new Stats();
        List<Thread> producerThreads = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();

        long startNs = System.nanoTime();

        // Start consumers
        for (int c = 0; c < NUM_CONSUMERS; c++) {
            Thread t = new Thread(new Consumer(queue, c, stats), "consumer-" + c);
            consumerThreads.add(t);
            t.start();
        }

        // Start producers with unique order IDs
        for (int p = 0; p < NUM_PRODUCERS; p++) {
            int startId = p * ORDERS_PER_PRODUCER;
            Thread t = new Thread(
                    new Producer(queue, startId, ORDERS_PER_PRODUCER),
                    "producer-" + p);
            producerThreads.add(t);
            t.start();
        }

        // Wait until every producer has finished
        for (Thread t : producerThreads) {
            joinOrFail(t);
        }

        // One poison pill per consumer (after all real orders are queued)
        for (int c = 0; c < NUM_CONSUMERS; c++) {
            queue.put(Order.POISON);
        }

        // Wait until every consumer has stopped
        for (Thread t : consumerThreads) {
            joinOrFail(t);
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        int expected = NUM_PRODUCERS * ORDERS_PER_PRODUCER;

        System.out.printf("Result for %-24s -> processed=%d, distinct=%d, "
                        + "poisonExits=%d, duplicates=%d, time=%d ms%n",
                label, stats.processedCount.get(), stats.processedIds.size(),
                stats.poisonExits.get(), stats.duplicates.get(), elapsedMs);

        // Check 2: every order processed exactly once
        if (stats.processedCount.get() != expected) {
            throw new AssertionError("Expected " + expected
                    + " orders, processed " + stats.processedCount.get());
        }
        if (stats.processedIds.size() != expected) {
            throw new AssertionError("Expected " + expected
                    + " distinct IDs, got " + stats.processedIds.size());
        }
        if (stats.duplicates.get() != 0) {
            throw new AssertionError("Duplicate orders: " + stats.duplicates.get());
        }
        // Check 2: every consumer terminated through a poison pill
        if (stats.poisonExits.get() != NUM_CONSUMERS) {
            throw new AssertionError("Only " + stats.poisonExits.get() + " of "
                    + NUM_CONSUMERS + " consumers exited via poison pill");
        }
    }

    private static void joinOrFail(Thread t) throws InterruptedException {
        t.join(JOIN_TIMEOUT_MS);
        if (t.isAlive()) {
            throw new AssertionError(t.getName() + " did not terminate (hang/deadlock)");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Starting 20-run Loop Test ===");

        for (int run = 1; run <= 20; run++) {
            System.out.println("\n--- RUN " + run + " ---");
            runScenario("Custom BoundedBuffer", new BoundedBuffer(BUFFER_CAPACITY));
            runScenario("Java ArrayBlockingQueue", new BlockingOrderQueue(BUFFER_CAPACITY));
        }

        System.out.println("\nSUCCESS: All 20 runs completed with 100 orders each, "
                + "each processed exactly once.");
    }
}

/** Order record. POISON is a sentinel compared by reference. */
record Order(int id, String item, int qty) {
    static final Order POISON = new Order(-1, "POISON", 0);
}

/** Shared counters for one scenario run. */
final class Stats {
    final AtomicInteger processedCount = new AtomicInteger();
    final AtomicInteger poisonExits = new AtomicInteger();
    final AtomicInteger duplicates = new AtomicInteger();
    final Set<Integer> processedIds = ConcurrentHashMap.newKeySet();
}

/** Common interface for both buffer implementations. */
interface OrderQueue {
    void put(Order order) throws InterruptedException;

    Order take() throws InterruptedException;
}

/** Manually implemented bounded buffer (monitor-based). */
class BoundedBuffer implements OrderQueue {

    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        this.capacity = capacity;
    }

    @Override
    public synchronized void put(Order order) throws InterruptedException {
        Objects.requireNonNull(order, "order");

        while (items.size() == capacity) {   // while, not if: spurious/stolen wakeups
            wait();
        }

        items.addLast(order);

        assert items.size() <= capacity : "buffer exceeded capacity";
        if (items.size() > capacity) {       // always-on check (works without -ea)
            throw new IllegalStateException("buffer exceeded capacity");
        }

        notifyAll();                         // producers and consumers share this monitor
    }

    @Override
    public synchronized Order take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }

        assert !items.isEmpty() : "take() on empty buffer";
        if (items.isEmpty()) {               // always-on check
            throw new IllegalStateException("take() on empty buffer");
        }

        Order order = items.removeFirst();
        notifyAll();
        return order;
    }
}

/** Java's built-in blocking queue, wrapped behind the same interface. */
final class BlockingOrderQueue implements OrderQueue {

    private final BlockingQueue<Order> queue;

    public BlockingOrderQueue(int capacity) {
        queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public void put(Order order) throws InterruptedException {
        queue.put(order);
    }

    @Override
    public Order take() throws InterruptedException {
        return queue.take();
    }
}

/** Producer: unchanged for both queue implementations. */
class Producer implements Runnable {

    private final OrderQueue queue;
    private final int startId;
    private final int ordersToProduce;

    public Producer(OrderQueue queue, int startId, int ordersToProduce) {
        this.queue = queue;
        this.startId = startId;
        this.ordersToProduce = ordersToProduce;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < ordersToProduce; i++) {
                int id = startId + i;
                int qty = ThreadLocalRandom.current().nextInt(1, 6);
                queue.put(new Order(id, "item-" + id, qty));
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 21));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/** Consumer: unchanged for both queue implementations. */
class Consumer implements Runnable {

    private final OrderQueue queue;
    private final int id;
    private final Stats stats;

    public Consumer(OrderQueue queue, int id, Stats stats) {
        this.queue = queue;
        this.id = id;
        this.stats = stats;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Order order = queue.take();

                if (order == Order.POISON) {          // terminate via poison pill
                    stats.poisonExits.incrementAndGet();
                    break;
                }

                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 31));

                if (!stats.processedIds.add(order.id())) {
                    stats.duplicates.incrementAndGet(); // same ID seen twice
                }
                stats.processedCount.incrementAndGet();

                if (BenchTask3.VERBOSE) {
                    System.out.printf("Consumer-%d processed %s%n", id, order);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}