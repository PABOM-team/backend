package com.pabom.backend.record.presentation.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.pabom.backend.record.domain.type.FeedingOutcome;
import com.pabom.backend.record.domain.type.TimePrecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateRecordRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID clientRequestId,
        @NotNull @Positive @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long petId,
        @NotBlank @Size(max = 50) @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "FEEDING") String itemCode,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time") OffsetDateTime occurredAt,
        @Schema(nullable = true, allowableValues = {"EXACT", "DATE_ONLY"}, defaultValue = "EXACT") TimePrecision timePrecision,
        @Schema(nullable = true, allowableValues = {"ATE_ALL", "ATE_PART", "REFUSED", "NOT_OBSERVED"}) FeedingOutcome outcome,
        @Schema(nullable = true) BigDecimal valueNum,
        @Schema(nullable = true, description = "SHED: stage(STARTED|FINISHED|NOT_SURE), observedDirectly(boolean)") JsonNode detail,
        @Size(max = 500) @Schema(nullable = true, maxLength = 500) String memo,
        @Positive @Schema(nullable = true) Long photoFileId,
        @Min(0) @Max(600000) @Schema(nullable = true, minimum = "0", maximum = "600000") Integer uxDurationMs
) {}
