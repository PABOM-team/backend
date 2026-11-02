package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutineDueDateCalculatorTest {
    private final RoutineDueDateCalculator calculator = new RoutineDueDateCalculator();

    @Test
    void calculatesRegistrationDateItemsInAsiaSeoul() {
        Instant registeredAt = Instant.parse("2026-11-01T15:30:00Z");
        Instant expected = Instant.parse("2026-11-02T03:00:00Z");

        for (String code : new String[] {"FEED", "WATER", "MIST", "HUMID_HIDE", "WEIGHT"}) {
            assertThat(calculator.firstDueAt(code, true, 3, registeredAt)).as(code).isEqualTo(expected);
        }
    }

    @Test
    void calculatesRegistrationDatePlusIntervalItems() {
        Instant registeredAt = Instant.parse("2026-11-01T15:30:00Z");
        Map<String, Integer> intervals = Map.of("CLEAN", 30, "SHED_CHECK", 7, "MULTIVITAMIN", 14);

        assertThat(intervals).allSatisfy((code, interval) ->
                assertThat(calculator.firstDueAt(code, true, interval, registeredAt)).as(code)
                        .isEqualTo(LocalDate.of(2026, 11, 2).plusDays(interval)
                                .atTime(12, 0).atZone(RoutineDueDateCalculator.SERVICE_ZONE).toInstant()));
    }

    @Test
    void keepsDefaultDisabledItemsUnscheduledUntilEnabled() {
        Instant registeredAt = Instant.parse("2026-11-01T15:30:00Z");

        assertThat(calculator.firstDueAt("FEED_INSECT", false, 7, registeredAt)).isNull();
        assertThat(calculator.firstDueAt("UVB_LAMP", false, 180, registeredAt)).isNull();
        assertThat(calculator.firstDueAfterEnable("FEED_INSECT", 7, registeredAt))
                .isEqualTo(Instant.parse("2026-11-09T03:00:00Z"));
        assertThat(calculator.firstDueAfterEnable("UVB_LAMP", 180, registeredAt))
                .isEqualTo(Instant.parse("2027-05-01T03:00:00Z"));
    }

    @Test
    void calculatesNextDateFromActualCompletionDateForFutureCompletionApi() {
        assertThat(calculator.nextDueAfterCompletion(LocalDate.of(2026, 11, 4), 3))
                .isEqualTo(Instant.parse("2026-11-07T03:00:00Z"));
    }

    @Test
    void doesNotInventScheduleForUnknownItemCode() {
        Instant anchor = Instant.parse("2026-11-01T15:30:00Z");
        assertThat(calculator.firstDueAt("FUTURE_ITEM", true, 5, anchor)).isNull();
        assertThat(calculator.firstDueAfterEnable("FUTURE_ITEM", 5, anchor)).isNull();
    }
}
