package com.pabom.backend.careguideline.presentation.response;

import com.pabom.backend.careguideline.domain.type.CareGuidelineImportMode;
import com.pabom.backend.careguideline.domain.type.GuidelineVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CareGuidelineImportResponse(
        @Schema(example = "1.2") String versionLabel,
        CareGuidelineImportMode mode,
        boolean valid,
        boolean persisted,
        GuidelineVersionStatus status,
        ImportCounts counts
) {
    public record ImportCounts(int stages, int items, int standards, int stageThresholds, int descriptions) {}
}
