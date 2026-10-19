
/*
 * LongAdder can be faster than AtomicLong under heavy contention because
 * it spreads updates across multiple internal cells instead of making
 * every thread update the same single value.
 */

package edu.itc.se.lab01.task2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;



public class Bench {
    static final int THREADS = 8;
    static final int ITERATIONS = 1_000_000;

    static void run(String label, Counter counter) throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) counter.increment();
            }, "inc-" + t);
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();
        long ms = (System.nanoTime() - start) / 1_000_000;
        long expected = (long) THREADS * ITERATIONS;
        System.out.printf("%-22s value=%,d  correct=%-5b  %d ms%n",
                label, counter.get(), counter.get() == expected, ms);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("---Running Unsafe Counter Experiments---");

        run("UnsafeCounter (Run1 )", new UnsafeCounter());
        run("UnsafeCounter (Run2 )", new UnsafeCounter());
        run("UnsafeCounter (Run3 )", new UnsafeCounter());
        
        System.out.println("\n---Running Safe Counter Benchmarks---");
        // TODO: SynchronizedCounter, LockCounter, AtomicCounter, LongAdderCounter
        // Run correct version 
        run("SychrinizedCounter" ,new SynchronizedCounter());
        run("LockCounter ", new LockCounter());
        run("AtomicCounter" ,new AtomicCounter());

        // Run LongAdder Bonus
        run("LongAdderCounter" , new LongAdderCounter());
    }
}

// Define an interface Counter
interface Counter {
    void increment();
    long get();
}

// Implement UnsafeCounter
class UnsafeCounter implements Counter {
    private long count =0 ;

    @Override 
    public void increment(){
        count++;
    }

    @Override 
    public  long get(){
        return count;
    }
}

// Implement SychronizedCounter
class SynchronizedCounter implements Counter {
    private long count =0 ;

    @Override 
    public synchronized void increment(){
        count++;
    }
    @Override 
    public synchronized long get(){
        return count;
    }
}

// Implement LockCounter
class LockCounter implements Counter {
    private long count =0 ;
    private final ReentrantLock lock = new ReentrantLock();

    @Override 
    public void increment(){
        lock.lock();
        try{
            count++;
        }finally{
            lock.unlock();
        }
    }
    @Override 
    public long get(){
        lock.lock();
        try{
            return count;
        }finally{
            lock.unlock();
        }
    }
}

// Implement AtomicCounter
class AtomicCounter implements Counter {
    private final AtomicLong count = new AtomicLong(0);

    @Override 
    public void increment(){
        count.incrementAndGet();
    }
    @Override 
    public long get(){
        return count.get();
    }
}

// Bonus : Implement LongAdderCounter
class LongAdderCounter implements Counter {
    private final LongAdder count = new LongAdder();

    @Override 
    public void increment (){
        count.increment();
    }

    @Override 
    public long get(){
        return count.sum();
    }
}
