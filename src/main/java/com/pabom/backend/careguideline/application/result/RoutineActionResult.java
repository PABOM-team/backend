package com.pabom.backend.careguideline.application.result;

import com.pabom.backend.record.domain.type.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RoutineActionResult(Record record, RoutineState routine,
        @Schema(nullable = true) TodayTaskStatus todayStatus) {
    public record Record(Long id, RecordType recordType, String itemCode, OffsetDateTime occurredAt,
            TimePrecision timePrecision, @Schema(nullable = true) FeedingOutcome outcome,
            @Schema(nullable = true) BigDecimal valueNum, @Schema(nullable = true) String unit,
            @Schema(nullable = true) SkipReasonCode reasonCode, @Schema(nullable = true) String memo) {}
    public record RoutineState(Long id, @Schema(nullable = true) LocalDate lastDoneOn,
            @Schema(nullable = true) LocalDate nextDueOn) {}
}
