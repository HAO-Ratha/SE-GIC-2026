package edu.itc.se.lab01.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WordCountTest {

    @TempDir
    Path tmp;

    @Test
    void parallelResultEqualsReference() throws Exception {
        Path dir = tmp.resolve("fixture");
        DataGenerator.generate(dir, 20, 5_000, 2_000, 1L);
        Map<String, Long> expected = ReferenceCounter.count(dir);

        for (boolean virtual : new boolean[] {false, true}) {
            WordCount wc = new WordCount(dir, 4, virtual, 10, 0);
            wc.csvPath = tmp.resolve("result-" + virtual + ".csv");
            wc.run();
            assertNoStrayThreads();

            assertFalse(wc.wasPartial(), "run should complete (virtual=" + virtual + ")");
            assertEquals(expected, wc.snapshot(), "maps differ (virtual=" + virtual + ")");
        }
    }

    @Test
    void stopsWithinThreeSecondsOnTimeout() throws Exception {
        Path dir = tmp.resolve("many");
        DataGenerator.generate(dir, 5_000, 100, 2_000, 2L);

        WordCount wc = new WordCount(dir, 2, false, 5, 1);   // --timeout 1
        wc.csvPath = tmp.resolve("partial.csv");
        wc.testDelayPerFileMs = 10;    // 5000 files x 10 ms / 2 workers ~ 25 s, so it must be cancelled

        long t0 = System.nanoTime();
        wc.run();
        assertNoStrayThreads();
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertTrue(wc.wasPartial(), "run should have been cut short");
        assertTrue(ms < 3_000, "took " + ms + " ms, expected < 3000");
    }

    /** After run() returns, no scanner / reporter / worker thread may still be alive. */
    private static void assertNoStrayThreads() throws InterruptedException {
        long deadline = System.nanoTime() + 1_000_000_000L;
        List<String> stray;
        do {
            stray = Thread.getAllStackTraces().keySet().stream()
                    .filter(Thread::isAlive)
                    .map(Thread::getName)
                    .filter(n -> n.equals("scanner") || n.equals("reporter") || n.startsWith("wc-worker-"))
                    .toList();
            if (stray.isEmpty()) return;
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        fail("stray threads still alive: " + stray);
    }
}