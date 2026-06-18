package com.goaway.contexts.mood.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SoupServiceTest {

    private final SoupService service = new SoupService();

    @Test
    @DisplayName("daily 同一天稳定、跨天可能不同")
    void daily_stableWithinDay() {
        LocalDate d = LocalDate.of(2026, 6, 18);
        assertEquals(service.daily(d), service.daily(d), "同一天应返回同一条");
        assertNotNull(service.daily(d));
        assertFalse(service.daily(d).isBlank());
    }

    @Test
    @DisplayName("random 返回语料中的一条")
    void random_returnsNonBlank() {
        for (int i = 0; i < 20; i++) {
            assertNotNull(service.random());
            assertFalse(service.random().isBlank());
        }
    }
}
