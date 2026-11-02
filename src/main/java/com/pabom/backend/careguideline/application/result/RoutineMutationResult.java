package com.pabom.backend.careguideline.application.result;

import com.pabom.backend.record.domain.type.CycleSource;
import java.time.LocalDate;

public record RoutineMutationResult(RoutineDetailsResult routine, Previous previous) {
    public record Previous(Integer intervalDays, boolean isEnabled, LocalDate startDate,
            CycleSource cycleSource, String versionLabel) {}
}
