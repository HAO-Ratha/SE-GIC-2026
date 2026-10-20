# Task 2 — Counter Race and Synchronization

## Lost-update race

`UnsafeCounter` uses:

    count++;

This looks like one operation, but it is actually approximately:

1. Read `count`
2. Add 1
3. Write the new value

When multiple threads execute this at the same time, two threads can read
the same old value and then write the same new value.

For example:

    Thread A reads 10
    Thread B reads 10
    Thread A writes 11
    Thread B writes 11

Two increments happened, but the counter only increased by 1.

The expected result is:

    8 × 1,000,000 = 8,000,000

`UnsafeCounter` can produce a smaller value because of these lost updates.

The exact wrong result changes between runs because it depends on thread
scheduling.

## Correct implementations

### SynchronizedCounter

`synchronized` allows only one thread at a time to execute the critical
section.

### LockCounter

`ReentrantLock` provides explicit locking. The lock is released in a
`finally` block so that it is not accidentally left locked.

### AtomicCounter

`AtomicLong` provides atomic operations such as `incrementAndGet()`
without manually using `synchronized`.

### LongAdder

`LongAdder` can perform better than `AtomicLong` under heavy contention.
Instead of having every thread repeatedly update one shared value,
LongAdder can distribute updates across multiple internal cells and
combine them when `sum()` is called.

## Why must get() also be synchronized?

Making only `increment()` synchronized protects the update operation,
but `get()` can still access the shared variable without synchronization.

For example:

    public synchronized void increment() {
        count++;
    }

    public long get() {
        return count;
    }

The increment is protected, but `get()` is not using the same
synchronization mechanism.

This means another thread could read the value while updates are
happening, and the program does not have a consistent synchronization
relationship between the read and the writes.

Therefore, both `increment()` and `get()` should be synchronized when
using this design.

## Performance

The benchmark uses:

    8 threads
    1,000,000 increments per thread
    8,000,000 total increments

The elapsed time is measured using `System.nanoTime()`.

Example output:

    UnsafeCounter          value=7,xxx,xxx  correct=false  XX ms
    SynchronizedCounter   value=8,000,000  correct=true   XX ms
    LockCounter            value=8,000,000  correct=true   XX ms
    AtomicCounter          value=8,000,000  correct=true   XX ms
    LongAdderCounter       value=8,000,000  correct=true   XX ms

Performance depends on the computer, JVM, and thread scheduling, so the
measured milliseconds can change between runs.


# Task 3  — Producer/Consumer with a Bounded Buffer

## Run
    javac -d out BenchTask3.java
    java -ea -cp out edu.itc.se.lab01.task3.BenchTask3

## Why `while` and not `if`
A thread that wakes from `wait()` is not guaranteed the condition it
waited for is true:
- **Spurious wakeups:** the JVM may wake a waiting thread for no reason.
- **Stolen condition:** with several producers and consumers, another
  thread may acquire the monitor first and consume the slot or item
  that triggered the wakeup.

With `if`, the woken thread would continue and could put into a full
buffer or take from an empty one. With
`while (items.size() == capacity) wait();` it re-checks the condition
after every wakeup and only proceeds when it is really true.

## Why `notifyAll()` and not `notify()`
Producers and consumers wait on the same monitor, but for different
conditions (not full / not empty). `notify()` wakes one arbitrary waiter.
If it wakes the wrong kind, for example a producer when the buffer is
still full, no thread makes progress and the others stay waiting
forever (deadlock). `notifyAll()` wakes everyone; each re-checks its own
condition in its `while` loop, and the right ones proceed.

## Termination
After all producers finish, one poison pill (`Order` with id -1) is sent
per consumer. Each consumer exits after taking exactly one pill.

## Verification
- Assertions: the buffer never exceeds capacity and `take()` never
  returns from an empty buffer.
- 100 orders are processed exactly once (count and distinct IDs).
- Every consumer exits via a poison pill.
- `Producer` and `Consumer` run unchanged on `BoundedBuffer` and
  `ArrayBlockingQueue` through the `OrderQueue` interface.


# Task 4 — ExecutorService, Future, Virtual Threads

## Run (JDK 21+)
    java -ea src\main\java\edu\itc\se\lab01\task4\Main.java

## Platform pool vs virtual threads (I/O-bound)
Each download only *waits* 200 ms; it does no computing.

- **20 platform threads:** each task blocks one OS thread, so only 20 run at
  once. 500 tasks = 25 waves x 200 ms, about **5 s**.
- **Virtual threads:** a sleeping virtual thread is unmounted from its carrier
  thread, so all 500 waits overlap. Total is about **0.2-0.4 s**.

Virtual threads do not help CPU-bound work (primes), where parallelism is
capped by the core count. Do not pool virtual threads; use one per task.

## Notes
- **Failures:** a task exception is stored in the `Future` and rethrown by
  `get()` as `ExecutionException`; `getCause()` gives the `IOException`.
- **Timeouts:** `get(timeout)` only stops waiting. `cancel(true)` interrupts
  the task so the executor can close without waiting the full 5 s.
- **Shutdown:** try-with-resources calls `close()`. An unclosed platform pool
  (non-daemon threads) would keep the JVM alive.
- **CPU split:** equal chunks have unequal work (larger numbers cost more),
  so the speedup is below the core count.


  # Task 5 : Deadlock, Lock Ordering, tryLock, Interruption

## Run (JDK 17+)
    java -ea src\main\java\edu\itc\se\lab01\task5\Bank.java broken   (hangs, Ctrl+C)
    java -ea src\main\java\edu\itc\se\lab01\task5\Bank.java          (fixes + shutdown)

## Deadlock
`transfer(A, B)` holds A and waits for B while `transfer(B, A)` holds B and
waits for A: a circular wait. The watchdog reported
`transfer-0, transfer-1`; `jstack` output is in `deadlock.txt`.

## Fixes
- **OrderedBank:** always lock the lower `id` first, so no cycle can form.
- **TryLockBank:** `tryLock(10 ms)` on both locks; on failure release, sleep
  1-5 ms (random) and retry. Retries are counted.

## Shutdown
Workers loop on `!isInterrupted()` and re-set the flag after
`InterruptedException`. `main` interrupts after 2 s and joins with a 1 s budget.

## Results
| Version | Transfers | Retries | Total | Time |
|---|---|---|---|---|
| OrderedBank | 800000 | – | 4000 | ___ ms |
| TryLockBank | 800000 | ___ | 4000 | ___ ms |



# Parallel Word-Frequency Analyser

Scans a directory of text files in parallel and reports the most frequent words.
JDK 21+, no external libraries (JUnit 6 for tests only).

## Run
    javac -d out src\main\java\edu\itc\se\lab01\challenge\*.java
    java -cp out edu.itc.se.lab01.challenge.DataGenerator data
    java -cp out edu.itc.se.lab01.challenge.WordCount data --top 10 --timeout 30

Options: `--top N`, `--timeout SECONDS`, `--virtual`, `--workers N`.
Output: console table and `result.csv`. Compile the main classes with the
command above; `WordCountTest` is under `src\test\java` and requires JUnit.

## Design
`scanner` → `ArrayBlockingQueue<Path>(16)` → N workers → `ConcurrentHashMap<String, LongAdder>`.
A reporter prints progress every 500 ms. Ctrl+C and `--timeout` call the same
`shutdown()`, which interrupts all threads, waits at most 2 s and prints a
`[PARTIAL]` result.

## Scaling (200 files x 50 000 words)
Generate with `java -cp out edu.itc.se.lab01.challenge.Benchmark data`.

| Workers | Elapsed (ms) | Speedup |
|---|---|---|
| 1 platform | ___ | x1.00 |
| 2 platform | ___ | ___ |
| 4 platform | ___ | ___ |
| 8 platform | ___ | ___ |
| virtual | ___ | ___ |

The work is CPU-bound, so virtual threads should match platform threads.

## Why LongAdder beats synchronized
A lock around the map lets one thread update at a time, and most increments hit
the same few hot words. `ConcurrentHashMap.get` is lock-free and `LongAdder`
spreads increments across per-thread cells, summed only when read. Workers
rarely block, and the reporter reads counters without stopping them.

## Coffman conditions
The design rules out **hold-and-wait** and **circular wait**: no thread holds
one lock while requesting another, and data flows one way (scanner → queue →
workers). Waiting is also abandonable: `put`, `take` and `sleep` respond to
`interrupt()`, so shutdown never waits on a stuck thread. Mutual exclusion
remains (queue slots), but it alone cannot cause a deadlock.