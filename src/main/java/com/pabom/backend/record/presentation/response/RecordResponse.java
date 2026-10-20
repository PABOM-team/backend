package com.pabom.backend.record.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.pabom.backend.record.domain.type.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecordResponse(
        Long id, Long petId, String petName, @Schema(nullable = true) Long routineId,
        String itemCode, String itemName, RecordType recordType, RecordSource source,
        OffsetDateTime occurredAt, TimePrecision timePrecision, OffsetDateTime createdAt,
        OffsetDateTime updatedAt, @Schema(nullable = true) FeedingOutcome outcome,
        @Schema(nullable = true) BigDecimal valueNum, @Schema(nullable = true) String unit,
        @Schema(nullable = true) JsonNode detail, @Schema(nullable = true) String memo,
        @Schema(nullable = true) PhotoResponse photo,
        @Schema(nullable = true) String standardVersionLabel, int revisionNo,
        @Schema(nullable = true) OffsetDateTime deletedAt,
        @Schema(nullable = true) OffsetDateTime restorableUntil
) {}
