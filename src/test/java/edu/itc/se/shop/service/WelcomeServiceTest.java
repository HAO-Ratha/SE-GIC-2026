package edu.itc.se.shop.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WelcomeServiceTest {

    static class FakeNotifier implements Notifier {
        final List<String> calls = new ArrayList<>();
        @Override
        public void send(String to, String text) { calls.add(to + "|" + text); }
    }

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-01-01T09:30:15Z"), ZoneOffset.UTC);

    @Test
    void normalWelcomeUsesDefaultNotifier() {
        FakeNotifier normal = new FakeNotifier();
        FakeNotifier urgent = new FakeNotifier();

        String text = new WelcomeService(normal, urgent, clock).welcome("Sok", false);

        assertEquals("Welcome Sok! It is 09:30:15", text);
        assertEquals(List.of("Sok|Welcome Sok! It is 09:30:15"), normal.calls);
        assertTrue(urgent.calls.isEmpty());
    }

    @Test
    void urgentWelcomeUsesUrgentNotifier() {
        FakeNotifier normal = new FakeNotifier();
        FakeNotifier urgent = new FakeNotifier();

        new WelcomeService(normal, urgent, clock).welcome("Dara", true);

        assertEquals(1, urgent.calls.size());
        assertTrue(normal.calls.isEmpty());
    }
}