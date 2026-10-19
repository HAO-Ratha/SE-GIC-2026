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



# Task 3 — Producer–Consumer

## How It Works

The program uses 2 producers to create 50 orders each and 3 consumers to process them. A bounded buffer with capacity 5 stores orders temporarily.

- **Producer:** Creates orders with random quantities and delays.
- **Buffer:** Stores orders and coordinates access between threads.
- **Consumer:** Takes and processes orders, then increments an `AtomicInteger`.
- **Poison pill:** An order with ID `-1` tells a consumer to stop.

The expected result is 100 processed orders.

## BoundedBuffer: `wait()` and `notifyAll()`

The manual buffer uses `synchronized` to protect shared data.

- `wait()` pauses a producer when the buffer is full or a consumer when it is empty.
- `notifyAll()` wakes waiting threads after the buffer changes.
- `while` rechecks the condition after waking up, ensuring the thread proceeds only when it is safe.

## BlockingQueue

`ArrayBlockingQueue` provides built-in synchronization through `put()` and `take()`. It replaces much of the manual waiting and notification code, reducing complexity and the risk of synchronization bugs.

Both implementations use the same `OrderQueue` interface, so the producer and consumer logic does not need to change.

## Experiment: `notify()` vs `notifyAll()` and `if` vs `while`

Changing `notifyAll()` to `notify()` wakes only one waiting thread. It may wake a thread that cannot proceed while leaving another eligible thread asleep, potentially causing the program to stall.

Changing `while` to `if` checks the condition only once. After waking up, a thread might continue even though the buffer is still full or empty, causing incorrect behavior.

Repeat the experiment 20 times to observe possible failures. Results depend on thread scheduling, so failure is not guaranteed on every run.

## Conclusion

The manual buffer demonstrates thread synchronization and coordination. `ArrayBlockingQueue` achieves the same goal with less code and fewer opportunities for errors, making it a practical choice for most producer–consumer applications.