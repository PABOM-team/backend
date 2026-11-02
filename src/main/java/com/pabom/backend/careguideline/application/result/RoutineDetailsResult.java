package com.pabom.backend.careguideline.application.result;

import com.pabom.backend.record.domain.type.CycleSource;
import com.pabom.backend.record.domain.type.RoutineModificationReason;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RoutineDetailsResult(
        Long id,
        Long petId,
        String itemCode,
        String itemName,
        Integer intervalDays,
        boolean isEnabled,
        CycleSource cycleSource,
        LocalDate startDate,
        OffsetDateTime nextDueAt,
        String versionLabel,
        Integer defaultCycleDays,
        Integer minDays,
        Integer maxDays,
        Boolean isOutOfRange,
        RoutineModificationReason reason,
        String reasonNote,
        OffsetDateTime modifiedAt
) {}
