package com.pabom.backend.careguideline.application.result;

import com.pabom.backend.record.domain.type.CycleSource;
import java.time.OffsetDateTime;

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
        String guidelineVersion,
        String recordType,
        String description
) {}

