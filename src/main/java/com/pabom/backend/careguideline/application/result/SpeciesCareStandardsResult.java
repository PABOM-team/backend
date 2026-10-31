package com.pabom.backend.careguideline.application.result;

import java.util.List;

public record SpeciesCareStandardsResult(
        String guidelineVersion,
        Long speciesId,
        String speciesCode,
        String speciesName,
        List<LifeStageStandards> lifeStages
) {
    public record LifeStageStandards(
            String lifeStage,
            String appliedLifeStage,
            List<CareItemStandard> careItems
    ) {}

    public record CareItemStandard(
            String careItemCode,
            String careItemName,
            int defaultCycleDays,
            int minDays,
            int maxDays,
            boolean defaultEnabled,
            String recordType,
            String feedResultSet,
            String defaultFoodName,
            String evidenceLevel,
            String sourceRefs,
            String description
    ) {}
}

