package com.pabom.backend.record.presentation.response;

import com.pabom.backend.record.domain.type.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecordListItemResponse(Long id, Long petId, String petName, String itemCode,
        String itemName, RecordType recordType, OffsetDateTime occurredAt, TimePrecision timePrecision,
        OffsetDateTime createdAt, @Schema(nullable = true) FeedingOutcome outcome,
        boolean needsResult, @Schema(nullable = true) BigDecimal valueNum,
        @Schema(nullable = true) String unit, String detailSummary,
        @Schema(nullable = true) String memoPreview, boolean hasPhoto, boolean isEdited,
        RecordSource source, @Schema(nullable = true) OffsetDateTime deletedAt) {}
