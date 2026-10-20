package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Requires JDK 21+ (ExecutorService is AutoCloseable since 19, virtual threads final in 21). */
public class Main {

    static final long LIMIT = 4_000_000;
    static final int CHUNKS = 16;
    static final int DOWNLOADS = 500;
    static final int FAIL_EVERY = 50;
    static final int PLATFORM_THREADS = 20;

    // ---------------------------------------------------------------- tasks

    static long countPrimes(long from, long to) {
        long count = 0;
        for (long n = Math.max(from, 2); n < to; n++) {
            boolean prime = true;
            for (long d = 2; d * d <= n; d++) {
                if (n % d == 0) { prime = false; break; }
            }
            if (prime) count++;
        }
        return count;
    }

    /** Counts primes in [from, to). */
    static final class PrimeCounter implements Callable<Long> {
        private final long from, to;

        PrimeCounter(long from, long to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Long call() {
            return countPrimes(from, to);
        }
    }

    /** Simulated network call: sleeps, then either fails or returns "url -> length". */
    static final class FakeDownload implements Callable<String> {
        private final String url;
        private final long sleepMs;
        private final boolean fail;

        FakeDownload(String url) {
            this(url, 200, false);
        }

        FakeDownload(String url, long sleepMs, boolean fail) {
            this.url = url;
            this.sleepMs = sleepMs;
            this.fail = fail;
        }

        @Override
        public String call() throws IOException, InterruptedException {
            Thread.sleep(sleepMs);                 // interrupted by cancel(true)
            if (fail) throw new IOException("boom");
            return url + " -> " + url.length();
        }
    }

    static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    // ------------------------------------------------- step 1: CPU-bound part

    static void cpuBound() throws Exception {
        int threads = Runtime.getRuntime().availableProcessors();

        long t0 = System.nanoTime();
        long single = countPrimes(0, LIMIT);
        long singleMs = ms(t0);
        System.out.printf("single-threaded: %d primes in %d ms%n", single, singleMs);

        t0 = System.nanoTime();
        long total = 0;
        long poolMs;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Long>> futures = new ArrayList<>();
            long size = LIMIT / CHUNKS;
            for (int c = CHUNKS - 1; c >= 0; c--) {   // heaviest chunk first
                long from = c * size;
                long to = (c == CHUNKS - 1) ? LIMIT : from + size;
                futures.add(pool.submit(new PrimeCounter(from, to)));
            }
            for (Future<Long> f : futures) {
                total += f.get();
            }
            poolMs = ms(t0);
        }
        System.out.printf("pool (%d threads, %d chunks): %d primes in %d ms (%s, speedup x%.2f)%n",
                threads, CHUNKS, total, poolMs,
                total == single ? "matches single-threaded" : "MISMATCH!",
                (double) singleMs / Math.max(poolMs, 1));
    }

    // -------------------------------- steps 2 + 3: I/O-bound part + failures

    record Result(int ok, int failed, long ms) {}

    static Result runDownloads(String label, ExecutorService exec) {
        long t0 = System.nanoTime();

        List<Future<String>> futures = new ArrayList<>();
        for (int i = 1; i <= DOWNLOADS; i++) {
            boolean fail = i % FAIL_EVERY == 0;    // every 50th download fails
            futures.add(exec.submit(new FakeDownload("https://example.com/file" + i, 200, fail)));
        }

        int ok = 0, failed = 0;
        String firstError = null;
        for (Future<String> f : futures) {
            try {
                f.get();
                ok++;
            } catch (ExecutionException e) {       // task threw -> do not crash
                failed++;
                if (firstError == null) firstError = String.valueOf(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        long elapsed = ms(t0);
        System.out.printf("%-28s ok=%d failed=%d in %d ms%s%n", label, ok, failed, elapsed,
                firstError == null ? "" : "  (first error: " + firstError + ")");
        return new Result(ok, failed, elapsed);
    }

    static Result ioBound() {
        Result platformRes, virtualRes;
        try (ExecutorService platform = Executors.newFixedThreadPool(PLATFORM_THREADS)) {
            platformRes = runDownloads("platform pool (" + PLATFORM_THREADS + " threads):", platform);
        }
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            virtualRes = runDownloads("virtual threads:", virtual);
        }
        System.out.printf("virtual threads were %.1fx faster%n",
                (double) platformRes.ms() / Math.max(virtualRes.ms(), 1));
        return virtualRes;
    }

    // ------------------------------------------------------- step 4: timeouts

    static final AtomicInteger timedOut = new AtomicInteger();

    static String await(String name, Future<String> f) {
        try {
            return name + ": ok -> " + f.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);                        // interrupts the sleeping task
            timedOut.incrementAndGet();
            return name + ": timed out (cancelled=" + f.isCancelled() + ")";
        } catch (ExecutionException e) {
            return name + ": failed -> " + e.getCause();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            f.cancel(true);
            return name + ": interrupted";
        }
    }

    static void timeouts() {
        long t0 = System.nanoTime();
        try (ExecutorService exec = Executors.newFixedThreadPool(2)) {
            Future<String> fast = exec.submit(new FakeDownload("https://example.com/fast"));
            Future<String> slow = exec.submit(new FakeDownload("https://example.com/slow", 5_000, false));
            System.out.println(await("fast", fast));
            System.out.println(await("slow", slow));
        } // close() returns quickly because the slow task was interrupted by cancel(true)
        System.out.printf("timeout section finished in %d ms (would be ~5000 without cancel)%n", ms(t0));
    }

    // -------------------------------------------- step 6 (bonus): CompletableFuture

    static String callUnchecked(Callable<String> task) {
        try {
            return task.call();
        } catch (Exception e) {
            throw new CompletionException(e);      // Supplier cannot throw checked exceptions
        }
    }

    static void bonus() {
        AtomicInteger failed = new AtomicInteger();
        long t0 = System.nanoTime();

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (int i = 1; i <= DOWNLOADS; i++) {
                String url = "https://example.com/file" + i;
                FakeDownload task = new FakeDownload(url, 200, i % FAIL_EVERY == 0);

                futures.add(CompletableFuture
                        .supplyAsync(() -> callUnchecked(task), exec)
                        .handle((result, ex) -> {
                            if (ex == null) return result;
                            failed.incrementAndGet();
                            Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                                    ? ex.getCause() : ex;
                            return "FALLBACK for " + url + " (" + cause.getMessage() + ")";
                        }));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            int failures = failed.get();
            System.out.printf("CompletableFuture + virtual: ok=%d failed=%d in %d ms%n",
                    DOWNLOADS - failures, failures, ms(t0));
            futures.stream().map(CompletableFuture::join)
                    .filter(s -> s.startsWith("FALLBACK")).findFirst()
                    .ifPresent(s -> System.out.println("  sample fallback: " + s));
        }
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws InterruptedException {
        Result io = null;
        try {
            System.out.println("=== 1. CPU-bound ===");
            cpuBound();

            System.out.println("\n=== 2+3. I/O-bound, " + DOWNLOADS + " downloads, every "
                    + FAIL_EVERY + "th fails ===");
            io = ioBound();

            System.out.println("\n=== 4. Timeout ===");
            timeouts();

            System.out.println("\n=== 6. Bonus: CompletableFuture ===");
            bonus();
        } catch (Exception e) {                    // never end with a stack trace
            System.out.println("unexpected error: " + e);
        }

        System.out.println("\n=== SUMMARY ===");
        System.out.printf("downloads: ok=%d failed=%d | timed out=%d%n",
                io == null ? 0 : io.ok(), io == null ? 0 : io.failed(), timedOut.get());

        // Step 5: all executors were closed by try-with-resources; no System.exit anywhere.
        Thread.sleep(200); // let pool workers finish dying
        List<String> alive = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && !t.isDaemon() && t != Thread.currentThread())
                .map(Thread::getName).toList();
        System.out.println("non-daemon threads still alive besides main: " + alive);
        System.out.println("main finished - the JVM should now exit by itself.");
    }
}