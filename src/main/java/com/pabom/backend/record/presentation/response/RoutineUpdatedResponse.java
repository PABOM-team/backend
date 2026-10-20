package com.pabom.backend.record.presentation.response;

import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public record RoutineUpdatedResponse(Long routineId, @Schema(nullable = true) OffsetDateTime nextDueAt) {}
