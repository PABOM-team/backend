package com.pabom.backend.careguideline.presentation.request;

import com.pabom.backend.record.domain.type.SkipReasonCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record SkipRoutineRequest(
        @NotNull UUID clientRequestId,
        @Schema(nullable = true) LocalDate occurredOn,
        @Schema(nullable = true) SkipReasonCode reasonCode,
        @Size(max = 200) @Schema(nullable = true, maxLength = 200) String memo
) {}
