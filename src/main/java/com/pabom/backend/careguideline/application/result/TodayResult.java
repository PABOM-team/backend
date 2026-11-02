package com.pabom.backend.careguideline.application.result;

import com.pabom.backend.record.domain.type.FeedingOutcome;
import com.pabom.backend.record.domain.type.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record TodayResult(
        LocalDate date,
        Summary summary,
        TodayEmptyReason emptyReason,
        NextUpcoming nextUpcoming,
        List<PetTasks> pets
) {
    public record Summary(int dueCount, int overdueCount, int needsResultCount,
            int doneCount, int skippedCount, int reportCount) {}

    public record NextUpcoming(Long routineId, Long petId, String itemCode, LocalDate dueDate) {}

    public record PetTasks(Long petId, String name, String speciesCommonName,
            Photo photo, String stageNotice, List<Task> tasks) {}

    public record Photo(Long fileId) {}

    public record Task(Long routineId, String itemCode, String itemName,
            TodayTaskStatus status, LocalDate dueDate, Integer overdueDays,
            String resultType, String unit, LastRecord lastRecord) {}

    public record LastRecord(Long recordId, RecordType recordType, OffsetDateTime occurredAt,
            FeedingOutcome outcome, BigDecimal value, String unit) {}
}
