package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.entity.CareStandard;
import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.domain.type.GuidelineVersionStatus;
import com.pabom.backend.careguideline.infrastructure.persistence.CareGuidelineVersionJpaRepository;
import com.pabom.backend.careguideline.infrastructure.persistence.CareStandardJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.species.domain.entity.LifeStage;
import com.pabom.backend.species.infrastructure.persistence.LifeStageJpaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicableCareGuidelineService {
    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final CareGuidelineVersionJpaRepository versions;
    private final CareStandardJpaRepository standards;
    private final LifeStageJpaRepository lifeStages;
    private final Clock clock;

    public Optional<CareGuidelineVersion> findApplicable() {
        LocalDate today = LocalDate.now(clock.withZone(SERVICE_ZONE));
        List<CareGuidelineVersion> candidates =
                versions.findAllByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        GuidelineVersionStatus.PUBLISHED, today);
        if (candidates.isEmpty()) return Optional.empty();
        LocalDate latestDate = candidates.getFirst().getEffectiveFrom();
        if (candidates.stream().filter(version -> latestDate.equals(version.getEffectiveFrom())).count() > 1) {
            throw new BusinessException(CareGuidelineQueryErrorCode.GUIDELINE_VERSION_CONFLICT);
        }
        return Optional.of(candidates.getFirst());
    }

    public CareGuidelineVersion requireApplicable() {
        return findApplicable().orElseThrow(() ->
                new BusinessException(CareGuidelineQueryErrorCode.APPLICABLE_GUIDELINE_NOT_FOUND));
    }

    public LifeStage requireStage(String code) {
        return lifeStages.findByCode(code).orElseThrow(() ->
                new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING));
    }

    public LifeStage guidelineStage(Long actualStageId) {
        LifeStage actual = actualStageId == null ? requireStage("UNKNOWN") : lifeStages.findById(actualStageId)
                .orElseThrow(() -> new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING));
        if (actual.hasOwnGuidelineRules()) return actual;
        if (actual.getGuidelineFallbackStageId() == null) {
            throw new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING);
        }
        return lifeStages.findById(actual.getGuidelineFallbackStageId()).orElseThrow(() ->
                new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING));
    }

    public List<CareStandard> standards(Long versionId, Long speciesId, Long stageId) {
        return standards.findAllByGuidelineVersionIdAndSpeciesIdAndLifeStageId(versionId, speciesId, stageId);
    }
}

