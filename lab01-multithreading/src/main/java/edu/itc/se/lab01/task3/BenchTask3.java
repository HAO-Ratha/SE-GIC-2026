
package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchTask3 {

    private static final int NUM_PRODUCERS = 2;
    private static final int ORDERS_PER_PRODUCER = 50;
    private static final int NUM_CONSUMERS = 3;
    private static final int BUFFER_CAPACITY = 5;

    static void runScenario(String label, OrderQueue queue)
            throws InterruptedException {

        AtomicInteger processedCount = new AtomicInteger(0);

        List<Thread> producerThreads = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();

        // Start consumers
        for (int c = 0; c < NUM_CONSUMERS; c++) {
            Thread thread = new Thread(
                    new Consumer(queue, c, processedCount)
            );

            consumerThreads.add(thread);
            thread.start();
        }

        // Start producers with unique order IDs
        for (int p = 0; p < NUM_PRODUCERS; p++) {
            int startId = p * ORDERS_PER_PRODUCER;

            Thread thread = new Thread(
                    new Producer(queue, startId, ORDERS_PER_PRODUCER)
            );

            producerThreads.add(thread);
            thread.start();
        }

        // Wait until every producer has finished
        for (Thread thread : producerThreads) {
            thread.join();
        }

        // Send one poison pill to each consumer
        for (int c = 0; c < NUM_CONSUMERS; c++) {
            queue.put(new Order(-1, "POISON", 0));
        }

        // Wait until every consumer has stopped
        for (Thread thread : consumerThreads) {
            thread.join();
        }

        int expected = NUM_PRODUCERS * ORDERS_PER_PRODUCER;
        int actual = processedCount.get();

        System.out.println(
                "Result for " + label + " -> Total Processed: " + actual
        );

        if (actual != expected) {
            throw new AssertionError(
                    "Expected " + expected + " orders, but processed " + actual
            );
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Starting 20-run Loop Test ===");

        for (int run = 1; run <= 20; run++) {
            System.out.println("\n--- RUN " + run + " ---");

            runScenario(
                    "Custom BoundedBuffer",
                    new BoundedBuffer(BUFFER_CAPACITY)
            );

            runScenario(
                    "Java ArrayBlockingQueue",
                    new BlockingOrderQueue(BUFFER_CAPACITY)
            );
        }

        System.out.println(
                "\nSUCCESS: All 20 runs completed with 100 orders each."
        );
    }
}

// Order record
record Order(int id, String item, int qty) {}

// Common interface for both buffer implementations
interface OrderQueue {
    void put(Order order) throws InterruptedException;

    Order take() throws InterruptedException;
}

// Manually implemented bounded buffer
class BoundedBuffer implements OrderQueue {

    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than zero"
            );
        }

        this.capacity = capacity;
    }

    @Override
    public synchronized void put(Order order)
            throws InterruptedException {

        while (items.size() == capacity) {
            wait();
        }

        items.addLast(order);
        notifyAll();
    }

    @Override
    public synchronized Order take()
            throws InterruptedException {

        while (items.isEmpty()) {
            wait();
        }

        Order order = items.removeFirst();
        notifyAll();

        return order;
    }
}

// Java's built-in blocking queue
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

// Producer
class Producer implements Runnable {

    private final OrderQueue queue;
    private final int startId;
    private final int ordersToProduce;

    public Producer(
            OrderQueue queue,
            int startId,
            int ordersToProduce
    ) {
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

                Order order = new Order(id, "item-" + id, qty);
                queue.put(order);

                int pause = ThreadLocalRandom.current().nextInt(0, 21);
                Thread.sleep(pause);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Consumer
class Consumer implements Runnable {

    private final OrderQueue queue;
    private final int id;
    private final AtomicInteger processedCount;

    public Consumer(
            OrderQueue queue,
            int id,
            AtomicInteger processedCount
    ) {
        this.queue = queue;
        this.id = id;
        this.processedCount = processedCount;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Order order = queue.take();

                // Stop when a poison pill is received
                if (order.id() == -1) {
                    break;
                }

                int pause = ThreadLocalRandom.current().nextInt(5, 31);
                Thread.sleep(pause);

                processedCount.incrementAndGet();

                System.out.printf(
                        "Consumer-%d processed Order[id=%d, item=%s, qty=%d]%n",
                        id, order.id(), order.item(), order.qty()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
