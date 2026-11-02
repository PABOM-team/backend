package com.pabom.backend.careguideline.presentation.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.pabom.backend.record.domain.type.FeedingOutcome;
import com.pabom.backend.record.domain.type.TimePrecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompleteRoutineRequest(
        @NotNull UUID clientRequestId,
        @Schema(nullable = true) OffsetDateTime occurredAt,
        @Schema(nullable = true, defaultValue = "EXACT") TimePrecision timePrecision,
        @Schema(nullable = true) FeedingOutcome outcome,
        @Schema(nullable = true) BigDecimal valueNum,
        @Schema(nullable = true) JsonNode detail,
        @Size(max = 500) @Schema(nullable = true) String memo
) {}
