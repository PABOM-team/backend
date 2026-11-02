package com.pabom.backend.careguideline.application.result;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RoutinePostponeResult(RoutineState routine) {
    public record RoutineState(Long id, @Schema(nullable = true) LocalDate nextDueOn,
            @Schema(nullable = true) LocalDate baseDueOn,
            @Schema(nullable = true) LocalDate postponedTo,
            @Schema(nullable = true) OffsetDateTime postponedAt) {}
}
