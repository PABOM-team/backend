package com.pabom.backend.careguideline.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoutineDueDateCalculator {
    static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    static final LocalTime DATE_ONLY_TIME = LocalTime.NOON;

    private static final Set<String> DUE_ON_START_DATE = Set.of(
            "FEED", "WATER", "MIST", "HUMID_HIDE", "WEIGHT");
    private static final Set<String> DUE_AFTER_INTERVAL = Set.of(
            "CLEAN", "SHED_CHECK", "MULTIVITAMIN");
    private static final Set<String> DUE_AFTER_ENABLE_INTERVAL = Set.of(
            "FEED_INSECT", "UVB_LAMP");

    public Instant firstDueAt(String careItemCode, boolean active, int intervalDays, Instant registeredAt) {
        if (!active) return null;

        LocalDate registrationDate = toServiceDate(registeredAt);
        if (DUE_ON_START_DATE.contains(careItemCode)) {
            return toDateOnlyInstant(registrationDate);
        }
        if (DUE_AFTER_INTERVAL.contains(careItemCode)) {
            return toDateOnlyInstant(registrationDate.plusDays(intervalDays));
        }
        return null;
    }

    public Instant firstDueAfterEnable(String careItemCode, int intervalDays, Instant enabledAt) {
        if (!DUE_AFTER_ENABLE_INTERVAL.contains(careItemCode)) return null;
        return toDateOnlyInstant(toServiceDate(enabledAt).plusDays(intervalDays));
    }

    public Instant nextDueAfterCompletion(LocalDate actualCompletionDate, int intervalDays) {
        return toDateOnlyInstant(actualCompletionDate.plusDays(intervalDays));
    }

    public LocalDate toServiceDate(Instant instant) {
        return instant.atZone(SERVICE_ZONE).toLocalDate();
    }

    public Instant toDateOnlyInstant(LocalDate date) {
        return date.atTime(DATE_ONLY_TIME).atZone(SERVICE_ZONE).toInstant();
    }
}
