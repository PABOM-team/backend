package com.pabom.backend.record.application.service;

import com.pabom.backend.careguideline.application.service.ApplicableCareGuidelineService;
import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import com.pabom.backend.careguideline.domain.entity.CareStandard;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.careguideline.infrastructure.persistence.CareGuidelineVersionJpaRepository;
import com.pabom.backend.careguideline.infrastructure.persistence.CareStandardJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.error.RecordErrorCode;
import com.pabom.backend.record.domain.type.FeedingOutcome;
import com.pabom.backend.species.infrastructure.persistence.SpeciesJpaRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedingOutcomePolicy {
    private static final String CRESTED = "CRESTED";
    private static final String DIET = "DIET";
    private static final Set<FeedingOutcome> CRESTED_DIET_OUTCOMES = Set.of(
            FeedingOutcome.EATING_TRACE_FOUND,
            FeedingOutcome.EATING_TRACE_NOT_FOUND,
            FeedingOutcome.NOT_OBSERVED);
    private static final Set<FeedingOutcome> LEGACY_OUTCOMES = Set.of(
            FeedingOutcome.ATE_ALL,
            FeedingOutcome.ATE_PART,
            FeedingOutcome.REFUSED,
            FeedingOutcome.NOT_OBSERVED);

    private final ApplicableCareGuidelineService applicableGuidelines;
    private final CareStandardJpaRepository standards;
    private final CareGuidelineVersionJpaRepository versions;
    private final CareItemCodeMappingJpaRepository mappings;
    private final SpeciesJpaRepository species;

    public void validate(Pet pet, CareItem item, Long guidelineVersionId, FeedingOutcome outcome) {
        if (outcome == null) return;
        if (!item.isOutcomeAllowed()) throw new BusinessException(RecordErrorCode.OUTCOME_NOT_ALLOWED);

        Set<FeedingOutcome> allowed = isCrestedDiet(pet, item, guidelineVersionId)
                ? CRESTED_DIET_OUTCOMES : LEGACY_OUTCOMES;
        if (!allowed.contains(outcome)) throw new BusinessException(RecordErrorCode.OUTCOME_NOT_ALLOWED);
    }

    private boolean isCrestedDiet(Pet pet, CareItem item, Long guidelineVersionId) {
        boolean crested = species.findById(pet.getSpeciesId())
                .map(value -> CRESTED.equals(value.getCode()))
                .orElse(false);
        if (!crested) return false;

        Optional<CareGuidelineVersion> version = guidelineVersionId == null
                ? applicableGuidelines.findApplicable() : versions.findById(guidelineVersionId);
        return version.flatMap(value -> standard(pet, item, value.getId()))
                .map(this::isDiet).orElse(false);
    }

    private Optional<CareStandard> standard(Pet pet, CareItem item, Long versionId) {
        Long guidelineItemId = mappings.findByLegacyCareItemId(item.getId())
                .map(CareItemCodeMapping::getGuidelineCareItemId)
                .orElse(item.getId());
        Long stageId = applicableGuidelines.guidelineStage(pet.getLifeStageId()).getId();
        return standards.findByGuidelineVersionIdAndSpeciesIdAndLifeStageIdAndCareItemId(
                versionId, pet.getSpeciesId(), stageId, guidelineItemId);
    }

    private boolean isDiet(CareStandard standard) {
        return DIET.equals(standard.getFeedResultSet());
    }
}
