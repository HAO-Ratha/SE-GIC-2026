package edu.itc.se.lab01.challenge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parallel word-frequency analyser (JDK only, JDK 21+).
 *
 * <pre>
 * scanner thread --put--> BlockingQueue&lt;Path&gt;(16) --take--> N workers --> ConcurrentHashMap&lt;String, LongAdder&gt;
 *                                                          reporter thread reads counters every 500 ms
 * </pre>
 */
public class WordCount {

    private static final Path POISON = Path.of("__POISON__");
    private static final int QUEUE_CAPACITY = 16;
    private static final long GRACE_SECONDS = 2;

    record WordFreq(String word, long count) {}

    private static final Comparator<WordFreq> BEST_FIRST =
            Comparator.<WordFreq>comparingLong(WordFreq::count).reversed()
                    .thenComparing(WordFreq::word);
    private static final Comparator<WordFreq> WORST_FIRST = BEST_FIRST.reversed();

    // shared state
    private final BlockingQueue<Path> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final AtomicInteger filesQueued = new AtomicInteger();
    private final AtomicInteger filesDone = new AtomicInteger();
    private final AtomicInteger filesFailed = new AtomicInteger();
    private final LongAdder wordsDone = new LongAdder();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final CountDownLatch workersDone;
    private final CountDownLatch resultPrinted = new CountDownLatch(1);
    private volatile boolean scanDone;
    private volatile boolean partial;

    // configuration
    private final Path dir;
    private final int workers;
    private final boolean virtual;
    private final int topN;
    private final long timeoutSec;

    // package-private knobs for tests
    volatile Path csvPath = Path.of("result.csv");
    volatile long testDelayPerFileMs = 0;
    volatile boolean quiet;                  // no console output (used by Benchmark)
    private volatile long elapsedMs;

    // runtime objects
    private volatile long startNanos;
    private volatile Thread scanner;
    private volatile ExecutorService pool;
    private volatile ScheduledExecutorService reporter;

    // reporter-only state (single reporter thread)
    private long lastWords;
    private long lastNanos;

    WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec) {
        if (workers < 1) throw new IllegalArgumentException("workers must be >= 1");
        if (top < 1) throw new IllegalArgumentException("top must be >= 1");
        if (timeoutSec < 0) throw new IllegalArgumentException("timeout must be >= 0");
        this.dir = dir;
        this.workers = workers;
        this.virtual = virtual;
        this.topN = top;
        this.timeoutSec = timeoutSec;
        this.workersDone = new CountDownLatch(workers);
    }

    // ------------------------------------------------------------------ run

    void run() throws InterruptedException {
        startNanos = System.nanoTime();
        lastNanos = startNanos;

        pool = virtual ? Executors.newVirtualThreadPerTaskExecutor()
                       : Executors.newFixedThreadPool(workers,
                               Thread.ofPlatform().name("wc-worker-", 0).factory());
        scanner = Thread.ofPlatform().name("scanner").start(this::scan);
        reporter = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reporter");
            t.setDaemon(true);
            return t;
        });
        reporter.scheduleAtFixedRate(this::report, 500, 500, TimeUnit.MILLISECONDS);

        Thread hook = new Thread(this::shutdown, "shutdown-hook");   // Ctrl+C / SIGTERM
        Runtime.getRuntime().addShutdownHook(hook);

        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                try {
                    work();
                } finally {
                    workersDone.countDown();
                }
            });
        }

        boolean finished;
        if (timeoutSec > 0) {
            finished = workersDone.await(timeoutSec, TimeUnit.SECONDS);
        } else {
            workersDone.await();
            finished = true;
        }

        if (finished || workersDone.getCount() == 0) {
            complete();           // normal end
        } else {
            shutdown();           // deadline passed: same path as Ctrl+C
        }

        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down; hook is running
        }
    }

    // -------------------------------------------------------------- scanner

    private void scan() {
        try {
            try (Stream<Path> files = Files.walk(dir)) {
                Iterator<Path> it = files.filter(Files::isRegularFile).iterator();
                while (it.hasNext()) {
                    queue.put(it.next());              // blocks when 16 paths are waiting: back-pressure
                    filesQueued.incrementAndGet();
                }
            } catch (IOException | RuntimeException e) {   // always fall through to the poison pills
                System.err.println("[scanner] " + e.getMessage());
            }
            scanDone = true;
            for (int i = 0; i < workers; i++) {
                queue.put(POISON);                     // one pill per worker
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --------------------------------------------------------------- worker

    private void work() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Path p = queue.take();
                if (p == POISON) return;
                if (testDelayPerFileMs > 0) Thread.sleep(testDelayPerFileMs);
                if (!countFile(p)) return;             // interrupted mid-file
                filesDone.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();        // restore the flag
        }
    }

    /** @return false if the thread was interrupted and must stop. */
    private boolean countFile(Path p) {
        try (BufferedReader in = open(p)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) return false;
                wordsDone.add(countLine(line));
            }
            return true;
        } catch (ClosedByInterruptException e) {       // file channels close on interrupt
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            filesFailed.incrementAndGet();
            System.err.println("[worker] cannot read " + p + ": " + e.getMessage());
            return true;
        }
    }

    /** Splits on non-letters, lower-cases, counts. Returns the number of words in the line. */
    private int countLine(String line) {
        int words = 0;
        int i = 0, n = line.length();
        while (i < n) {
            int cp = line.codePointAt(i);
            if (!Character.isLetter(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            int start = i;
            do {
                i += Character.charCount(cp);
            } while (i < n && Character.isLetter(cp = line.codePointAt(i)));

            increment(line.substring(start, i).toLowerCase(Locale.ROOT));
            words++;
        }
        return words;
    }

    private void increment(String word) {
        LongAdder adder = counts.get(word);            // lock-free fast path
        if (adder == null) {
            adder = counts.computeIfAbsent(word, k -> new LongAdder());
        }
        adder.increment();
    }

    static BufferedReader open(Path p) throws IOException {
        // InputStreamReader replaces malformed bytes instead of throwing
        return new BufferedReader(
                new InputStreamReader(Files.newInputStream(p), StandardCharsets.UTF_8), 1 << 16);
    }

    // ------------------------------------------------------------- reporter

    private void report() {
        if (quiet) return;
        try {
            long now = System.nanoTime();
            long words = wordsDone.sum();
            double rate = (words - lastWords) * 1e9 / Math.max(1, now - lastNanos);
            lastWords = words;
            lastNanos = now;

            StringBuilder top3 = new StringBuilder();
            for (WordFreq wf : topWords(3)) {
                if (top3.length() > 0) top3.append(' ');
                top3.append(wf.word()).append('=').append(wf.count());
            }
            System.out.printf(Locale.ROOT, "[progress] files %d/%d%s | words %,d | %,.0f words/s | top3: %s%n",
                    filesDone.get(), filesQueued.get(), scanDone ? "" : "+", words, rate, top3);
        } catch (RuntimeException e) {
            // never let an exception cancel the periodic task
        }
    }

    /** Top-k words by count (ties by word). Reads the map weakly-consistently, no locks. */
    List<WordFreq> topWords(int k) {
        PriorityQueue<WordFreq> heap = new PriorityQueue<>(k + 1, WORST_FIRST);
        for (Map.Entry<String, LongAdder> e : counts.entrySet()) {
            WordFreq wf = new WordFreq(e.getKey(), e.getValue().sum());
            if (heap.size() < k) {
                heap.add(wf);
            } else if (BEST_FIRST.compare(wf, heap.peek()) < 0) {
                heap.poll();
                heap.add(wf);
            }
        }
        List<WordFreq> out = new ArrayList<>(heap);
        out.sort(BEST_FIRST);
        return out;
    }

    Map<String, Long> snapshot() {
        Map<String, Long> m = new HashMap<>();
        counts.forEach((w, a) -> m.put(w, a.sum()));
        return m;
    }

    boolean wasPartial() { return partial; }

    long elapsedMs() { return elapsedMs; }

    // ------------------------------------------------------------ shutdown

    /** Normal completion: all workers consumed their poison pill. */
    private void complete() throws InterruptedException {
        if (!stopping.compareAndSet(false, true)) {
            awaitResult();                             // shutdown() won the race and is printing
            return;
        }
        scanner.join(GRACE_SECONDS * 1000);
        pool.shutdown();
        pool.awaitTermination(GRACE_SECONDS, TimeUnit.SECONDS);
        reporter.shutdownNow();
        printResult(false);
    }

    /** Used by both the --timeout deadline and the JVM shutdown hook. */
    void shutdown() {
        if (!stopping.compareAndSet(false, true)) {
            awaitResult();
            return;
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GRACE_SECONDS);
        scanner.interrupt();
        pool.shutdownNow();                            // interrupts every worker
        try {
            scanner.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
            pool.awaitTermination(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reporter.shutdownNow();
        printResult(true);
    }

    private void awaitResult() {
        try {
            resultPrinted.await(GRACE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --------------------------------------------------------------- result

    void printResult(boolean isPartial) {
        try {
            partial = isPartial;
            elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            long total = wordsDone.sum();
            List<WordFreq> top = topWords(topN);
            String mode = virtual ? "virtual threads (" + workers + " workers)"
                                  : "platform threads (" + workers + " workers)";

            StringBuilder csv = new StringBuilder("rank,word,count,percent\n");
            StringBuilder out = new StringBuilder("\n");
            out.append(isPartial ? "[PARTIAL] stopped before all files were processed" : "Result").append('\n');
            out.append(String.format(Locale.ROOT,
                    "mode=%s | files=%d/%d (failed=%d) | words=%,d | elapsed=%,d ms%n",
                    mode, filesDone.get(), filesQueued.get(), filesFailed.get(), total, elapsedMs));
            int rank = 1;
            for (WordFreq wf : top) {
                double pct = total == 0 ? 0.0 : 100.0 * wf.count() / total;
                out.append(String.format(Locale.ROOT, "%3d. %-16s %,12d %6.2f%%%n", rank, wf.word(), wf.count(), pct));
                csv.append(String.format(Locale.ROOT, "%d,%s,%d,%.4f%n", rank, wf.word(), wf.count(), pct));
                rank++;
            }
            try {
                Files.writeString(csvPath, csv.toString(), StandardCharsets.UTF_8);
                out.append("written ").append(csvPath.toAbsolutePath()).append('\n');
            } catch (IOException e) {
                out.append("cannot write CSV: ").append(e.getMessage()).append('\n');
            }
            if (!quiet) System.out.print(out);
        } finally {
            resultPrinted.countDown();
        }
    }

    // ----------------------------------------------------------------- main

    /** --verify: parallel result vs single-threaded reference. */
    private static void verify(String[] a) throws Exception {
        if (a.length < 1) {
            System.err.println("usage: WordCount --verify <dir>");
            return;
        }
        Path d = Path.of(a[0]);
        Path csv = Files.createTempFile("wordcount", ".csv");
        long t0 = System.nanoTime();
        Map<String, Long> expected = ReferenceCounter.count(d);
        long refMs = (System.nanoTime() - t0) / 1_000_000;

        WordCount wc = new WordCount(d, Runtime.getRuntime().availableProcessors(), false, 10, 0);
        wc.csvPath = csv;
        wc.quiet = true;
        wc.run();
        Files.deleteIfExists(csv);

        boolean same = expected.equals(wc.snapshot());
        System.out.printf(Locale.ROOT, "single-threaded: %d ms | parallel: %d ms | maps %s (%d distinct words)%n",
                refMs, wc.elapsedMs(), same ? "IDENTICAL" : "DIFFERENT", expected.size());
    }

    private static void usage() {
        System.err.println("usage: WordCount <dir> [--top N] [--timeout SECONDS] [--virtual] [--workers N]");
        System.err.println("       WordCount --generate <dir> [files=200] [wordsPerFile=50000]");
        System.err.println("       WordCount --verify <dir>");
        System.err.println("       WordCount --bench <dir>");
    }

    private static String next(String[] a, int i) {
        if (i >= a.length) throw new IllegalArgumentException("missing value for " + a[i - 1]);
        return a[i];
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            switch (args[0]) {
                case "--generate" -> { DataGenerator.main(rest); return; }
                case "--verify"   -> { verify(rest); return; }
                case "--bench"    -> { Benchmark.main(rest); return; }
                default -> { }
            }
        }
        Path dir = null;
        int top = 10;
        long timeout = 0;
        boolean virtual = false;
        int workers = Runtime.getRuntime().availableProcessors();
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--top" -> top = Integer.parseInt(next(args, ++i));
                    case "--timeout" -> timeout = Long.parseLong(next(args, ++i));
                    case "--workers" -> workers = Integer.parseInt(next(args, ++i));
                    case "--virtual" -> virtual = true;
                    default -> {
                        if (args[i].startsWith("--") || dir != null) {
                            throw new IllegalArgumentException("unexpected argument " + args[i]);
                        }
                        dir = Path.of(args[i]);
                    }
                }
            }
            if (dir == null || !Files.isDirectory(dir)) {
                throw new IllegalArgumentException("directory required and must exist");
            }
        } catch (IllegalArgumentException e) {          // includes NumberFormatException
            System.err.println("error: " + e.getMessage());
            usage();
            System.exit(2);
            return;
        }
        new WordCount(dir, workers, virtual, top, timeout).run();
    }
}

/** Writes test files of random words from a fixed vocabulary (skewed so top words are meaningful). */
class DataGenerator {

    static void generate(Path dir, int files, int wordsPerFile, int vocabSize, long seed) throws IOException {
        Files.createDirectories(dir);
        List<String> vocab = vocabulary(vocabSize, seed);
        for (int f = 0; f < files; f++) {
            SplittableRandom rnd = new SplittableRandom(seed * 31 + f);
            Path file = dir.resolve(String.format("file-%05d.txt", f));
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                StringBuilder line = new StringBuilder();
                for (int w = 0; w < wordsPerFile; w++) {
                    int idx = (int) (vocabSize * Math.pow(rnd.nextDouble(), 3));   // skew to low indexes
                    line.append(vocab.get(idx)).append(' ');
                    if (w % 12 == 11 || w == wordsPerFile - 1) {
                        out.write(line.toString());
                        out.newLine();
                        line.setLength(0);
                    }
                }
            }
        }
    }

    private static List<String> vocabulary(int size, long seed) {
        SplittableRandom rnd = new SplittableRandom(seed);
        Set<String> words = new LinkedHashSet<>();
        while (words.size() < size) {
            int len = 3 + rnd.nextInt(8);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) sb.append((char) ('a' + rnd.nextInt(26)));
            words.add(sb.toString());
        }
        return new ArrayList<>(words);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: DataGenerator <dir> [files=200] [wordsPerFile=50000]");
            return;
        }
        Path dir = Path.of(args[0]);
        int files = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int words = args.length > 2 ? Integer.parseInt(args[2]) : 50_000;
        generate(dir, files, words, 2_000, 42L);
        System.out.println("wrote " + files + " files x " + words + " words to " + dir.toAbsolutePath());
    }
}

/** Single-threaded reference: plain HashMap, regex tokenizer (deliberately different from WordCount). */
class ReferenceCounter {

    private static final Pattern NON_LETTERS = Pattern.compile("[^\\p{L}]+");

    static Map<String, Long> count(Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(dir)) {
            files = s.filter(Files::isRegularFile).sorted().toList();
        }
        Map<String, Long> counts = new HashMap<>();
        for (Path p : files) {
            try (BufferedReader in = WordCount.open(p)) {
                String line;
                while ((line = in.readLine()) != null) {
                    for (String token : NON_LETTERS.split(line)) {
                        if (!token.isEmpty()) {
                            counts.merge(token.toLowerCase(Locale.ROOT), 1L, Long::sum);
                        }
                    }
                }
            }
        }
        return counts;
    }
}

/** Prints the README scaling table:  java -cp out edu.itc.se.lab01.challenge.Benchmark data */
class Benchmark {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: Benchmark <dir>");
            return;
        }
        Path dir = Path.of(args[0]);
        Path csv = Files.createTempFile("wordcount", ".csv");

        run(dir, 4, false, csv);                       // warm-up (JIT + file cache), not reported

        StringBuilder table = new StringBuilder("| Workers | Elapsed (ms) | Speedup |\n|---|---|---|\n");
        long base = 0;
        for (int w : new int[] {1, 2, 4, 8}) {
            long ms = run(dir, w, false, csv);
            if (w == 1) base = ms;
            table.append(String.format(Locale.ROOT, "| %d platform | %d | x%.2f |%n", w, ms, (double) base / Math.max(ms, 1)));
        }
        int cores = Runtime.getRuntime().availableProcessors();
        long ms = run(dir, cores, true, csv);
        table.append(String.format(Locale.ROOT, "| virtual (%d workers) | %d | x%.2f |%n", cores, ms, (double) base / Math.max(ms, 1)));

        System.out.println(table);
        Files.deleteIfExists(csv);
    }

    private static long run(Path dir, int workers, boolean virtual, Path csv) throws InterruptedException {
        WordCount wc = new WordCount(dir, workers, virtual, 10, 0);
        wc.csvPath = csv;
        wc.quiet = true;
        wc.run();
        return wc.elapsedMs();
    }
}