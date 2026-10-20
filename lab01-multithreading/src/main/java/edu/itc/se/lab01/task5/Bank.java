package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Usage:  java Bank.java [broken | ordered | trylock | shutdown [ordered]]
 * No argument runs ordered, trylock and shutdown (not broken, which hangs).
 */
public class Bank {

    static final int ACCOUNTS = 4;
    static final long INITIAL = 1_000;
    static final long EXPECTED = ACCOUNTS * INITIAL;
    static final int WORKERS = 8;                      // raise this if "broken" does not hang
    static final long TRANSFERS_PER_WORKER = 100_000;  // 8 x 100 000 = 800 000
    static final long SHUTDOWN_AFTER_MS = 2_000;

    // ------------------------------------------------------------ account

    static final class Account {
        final int id;
        private long balance;
        final ReentrantLock lock = new ReentrantLock();   // used only by TryLockBank

        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a)  { balance += a; }
    }

    static void move(Account from, Account to, long amount) {
        if (from.balance() >= amount) {
            from.withdraw(amount);
            to.deposit(amount);
        }
    }

    // --------------------------------------------------------- strategies

    interface TransferStrategy {
        void transfer(Account from, Account to, long amount) throws InterruptedException;
        default String stats() { return ""; }
    }

    /** BUG: lock order depends on argument order -> circular wait. */
    static final class BrokenBank implements TransferStrategy {
        @Override
        public void transfer(Account from, Account to, long amount) {
            synchronized (from) {
                synchronized (to) {
                    move(from, to, amount);
                }
            }
        }
    }

    /** Fix A: always lock the lower id first, so no circular wait is possible. */
    static final class OrderedBank implements TransferStrategy {
        @Override
        public void transfer(Account from, Account to, long amount) {
            Account first = from.id < to.id ? from : to;
            Account second = (first == from) ? to : from;
            synchronized (first) {
                synchronized (second) {
                    move(from, to, amount);
                }
            }
        }
    }

    /** Fix B: tryLock with timeout on both locks; on failure release everything, back off, retry. */
    static final class TryLockBank implements TransferStrategy {
        private final AtomicLong retries = new AtomicLong();

        @Override
        public void transfer(Account from, Account to, long amount) throws InterruptedException {
            while (true) {
                if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                    try {
                        if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                            try {
                                move(from, to, amount);
                                return;
                            } finally {
                                to.lock.unlock();
                            }
                        }
                    } finally {
                        from.lock.unlock();
                    }
                }
                retries.incrementAndGet();
                Thread.sleep(ThreadLocalRandom.current().nextInt(1, 6));   // random back-off
            }
        }

        @Override
        public String stats() { return "retries=" + retries.get() + " "; }
    }

    // ------------------------------------------------------------- worker

    static final class Worker implements Runnable {
        private final Account[] accounts;
        private final TransferStrategy bank;
        private final long limit;
        volatile long completed;                      // single writer: this worker
        volatile boolean interruptFlagKept;           // flag still set when run() ended

        Worker(Account[] accounts, TransferStrategy bank, long limit) {
            this.accounts = accounts;
            this.bank = bank;
            this.limit = limit;
        }

        @Override
        public void run() {
            var rnd = ThreadLocalRandom.current();
            try {
                while (!Thread.currentThread().isInterrupted() && completed < limit) {
                    int i = rnd.nextInt(accounts.length);
                    int j = rnd.nextInt(accounts.length - 1);
                    if (j >= i) j++;                  // j != i, so every iteration is a transfer
                    bank.transfer(accounts[i], accounts[j], rnd.nextInt(1, 50));
                    completed++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // restore the flag
            } finally {
                interruptFlagKept = Thread.currentThread().isInterrupted();
            }
        }
    }

    record Run(Account[] accounts, Worker[] workers, Thread[] threads) {
        static Run start(TransferStrategy bank, long limit) {
            Account[] accounts = new Account[ACCOUNTS];
            for (int i = 0; i < ACCOUNTS; i++) accounts[i] = new Account(i, INITIAL);
            Worker[] workers = new Worker[WORKERS];
            Thread[] threads = new Thread[WORKERS];
            for (int w = 0; w < WORKERS; w++) {
                workers[w] = new Worker(accounts, bank, limit);
                threads[w] = new Thread(workers[w], "transfer-" + w);
                threads[w].start();
            }
            return new Run(accounts, workers, threads);
        }

        long transfers() { long s = 0; for (Worker w : workers) s += w.completed; return s; }
        long total() { long s = 0; for (Account a : accounts) s += a.balance(); return s; }
    }

    // ----------------------------------------------------------- watchdog

    static void startWatchdog() {
        Thread t = new Thread(() -> {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            boolean reported = false;
            while (true) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    return;
                }
                long[] ids = mx.findDeadlockedThreads();
                if (ids != null && !reported) {
                    reported = true;
                    String names = Arrays.stream(mx.getThreadInfo(ids))
                            .filter(info -> info != null)
                            .map(ThreadInfo::getThreadName)
                            .sorted()
                            .reduce((a, b) -> a + ", " + b).orElse("?");
                    System.out.println("[watchdog] DEADLOCK detected between: " + names);
                }
            }
        }, "watchdog");
        t.setDaemon(true);                            // never keeps the JVM alive
        t.start();
    }

    // -------------------------------------------------------------- modes

    static void runBroken() throws InterruptedException {
        System.out.println("pid = " + ProcessHandle.current().pid()
                + "  -> in another terminal: jstack <pid> > jstack.txt");
        Run run = Run.start(new BrokenBank(), TRANSFERS_PER_WORKER);
        for (Thread t : run.threads()) t.join();      // hangs forever on deadlock
        System.out.println("total = " + run.total() + " (expected " + EXPECTED + ")");
    }

    static void runFixed(String label, TransferStrategy bank) throws InterruptedException {
        long t0 = System.nanoTime();
        Run run = Run.start(bank, TRANSFERS_PER_WORKER);
        for (Thread t : run.threads()) {
            t.join(30_000);
            if (t.isAlive()) throw new AssertionError(t.getName() + " still running after 30 s (hang?)");
        }
        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        long total = run.total();
        System.out.printf("%s: transfers=%d %stotal=%d (expected %d) in %d ms%n",
                label, run.transfers(), bank.stats(), total, EXPECTED, elapsed);
        if (total != EXPECTED) throw new AssertionError("balance changed: " + total);
    }

    static void runShutdown(String label, TransferStrategy bank) throws InterruptedException {
        System.out.println(label + ": graceful shutdown");
        Run run = Run.start(bank, Long.MAX_VALUE);    // run until interrupted
        Thread.sleep(SHUTDOWN_AFTER_MS);

        System.out.printf("[main] interrupting %d workers after %d ms%n", WORKERS, SHUTDOWN_AFTER_MS);
        long tInterrupt = System.nanoTime();
        for (Thread t : run.threads()) t.interrupt();

        boolean allStopped = true, flagsKept = true;
        for (int i = 0; i < WORKERS; i++) {
            Thread t = run.threads()[i];
            long elapsed = (System.nanoTime() - tInterrupt) / 1_000_000;
            t.join(Math.max(1, 1_000 - elapsed));     // 1 s budget counted from interrupt()
            elapsed = (System.nanoTime() - tInterrupt) / 1_000_000;
            if (t.isAlive()) {
                allStopped = false;
                System.out.println(t.getName() + " DID NOT STOP within 1 s");
            } else {
                flagsKept &= run.workers()[i].interruptFlagKept;
                System.out.println(t.getName() + " stopped within " + elapsed + " ms after "
                        + run.workers()[i].completed + " transfers");
            }
        }
        long total = run.total();
        System.out.printf("all workers stopped=%b interruptFlagKept=%b %stotal=%d (expected %d)%n",
                allStopped, flagsKept, bank.stats(), total, EXPECTED);
        if (!allStopped || !flagsKept || total != EXPECTED) {
            throw new AssertionError("shutdown check failed");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        startWatchdog();
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "broken"  -> runBroken();
            case "ordered" -> runFixed("OrderedBank", new OrderedBank());
            case "trylock" -> runFixed("TryLockBank", new TryLockBank());
            case "shutdown" -> {
                boolean ordered = args.length > 1 && args[1].equals("ordered");
                runShutdown(ordered ? "OrderedBank" : "TryLockBank",
                        ordered ? new OrderedBank() : new TryLockBank());
            }
            default -> {
                runFixed("OrderedBank", new OrderedBank());
                runFixed("TryLockBank", new TryLockBank());
                runShutdown("TryLockBank", new TryLockBank());
            }
        }
    }
}