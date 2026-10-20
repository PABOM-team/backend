package com.pabom.backend.record.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record DeleteRecordResponse(Long recordId, OffsetDateTime deletedAt,
        OffsetDateTime restorableUntil, @Schema(nullable = true) RoutineUpdatedResponse routineUpdated) {}
