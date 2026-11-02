package com.pabom.backend.careguideline.application.result;

import com.pabom.backend.record.domain.type.CycleSource;
import java.time.OffsetDateTime;
import java.time.LocalDate;

public record PetRoutineResult(
        Long routineId,
        Long petId,
        String careItemCode,
        String careItemName,
        boolean active,
        Integer intervalDays,
        CycleSource cycleSource,
        Integer defaultCycleDays,
        Integer minDays,
        Integer maxDays,
        Boolean isOutOfRange,
        OffsetDateTime nextDueAt,
        LocalDate startDate,
        String guidelineVersion,
        String recordType,
        String description
) {
    public PetRoutineResult(Long routineId, Long petId, String careItemCode, String careItemName,
            boolean active, Integer intervalDays, CycleSource cycleSource,
            Integer defaultCycleDays, Integer minDays, Integer maxDays, Boolean isOutOfRange,
            OffsetDateTime nextDueAt, String guidelineVersion, String recordType, String description) {
        this(routineId, petId, careItemCode, careItemName, active, intervalDays, cycleSource,
                defaultCycleDays, minDays, maxDays, isOutOfRange, nextDueAt, null,
                guidelineVersion, recordType, description);
    }
}

