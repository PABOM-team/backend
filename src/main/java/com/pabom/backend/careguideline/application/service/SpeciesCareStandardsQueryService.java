package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult;
import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult.CareItemStandard;
import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult.LifeStageStandards;
import com.pabom.backend.careguideline.domain.entity.*;
import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemDescriptionJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.species.domain.entity.LifeStage;
import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.domain.error.SpeciesErrorCode;
import com.pabom.backend.species.infrastructure.persistence.SpeciesJpaRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesCareStandardsQueryService {
    private final SpeciesJpaRepository speciesRepository;
    private final ApplicableCareGuidelineService applicableGuidelines;
    private final CareItemJpaRepository careItems;
    private final CareItemDescriptionJpaRepository descriptions;

    public SpeciesCareStandardsResult get(Long speciesId) {
        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(SpeciesErrorCode.SPECIES_NOT_FOUND));
        CareGuidelineVersion version = applicableGuidelines.requireApplicable();
        LifeStage juvenile = applicableGuidelines.requireStage("JUVENILE");
        LifeStage adult = applicableGuidelines.requireStage("ADULT");
        List<LifeStageStandards> stages = List.of(
                stage(version, species, "JUVENILE", juvenile),
                stage(version, species, "ADULT", adult),
                stage(version, species, "UNKNOWN", juvenile));
        return new SpeciesCareStandardsResult(version.getVersionLabel(), species.getId(), species.getCode(),
                species.getCommonName(), stages);
    }

    private LifeStageStandards stage(CareGuidelineVersion version, Species species,
            String requestedStage, LifeStage appliedStage) {
        List<CareStandard> standards = applicableGuidelines.standards(
                version.getId(), species.getId(), appliedStage.getId());
        if (standards.isEmpty()) {
            throw new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING);
        }
        Map<Long, CareItem> items = careItems.findAllById(
                        standards.stream().map(CareStandard::getCareItemId).toList()).stream()
                .collect(Collectors.toMap(CareItem::getId, Function.identity()));
        if (items.size() != standards.stream().map(CareStandard::getCareItemId).distinct().count()) {
            throw new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING);
        }
        List<CareItemStandard> results = standards.stream()
                .sorted(Comparator.comparing(standard -> items.get(standard.getCareItemId()).getCode()))
                .map(standard -> toItem(version.getId(), species.getId(), standard,
                        items.get(standard.getCareItemId())))
                .toList();
        return new LifeStageStandards(requestedStage, appliedStage.getCode(), results);
    }

    private CareItemStandard toItem(Long versionId, Long speciesId, CareStandard standard, CareItem item) {
        String description = descriptions.findByGuidelineVersionIdAndSpeciesIdAndCareItemId(
                        versionId, speciesId, item.getId())
                .or(() -> descriptions.findByGuidelineVersionIdAndSpeciesIdIsNullAndCareItemId(
                        versionId, item.getId()))
                .map(CareItemDescription::getDescriptionText).orElse(null);
        return new CareItemStandard(item.getCode(), item.getName(), standard.getDefaultCycleDays(),
                standard.getMinDays(), standard.getMaxDays(), standard.isDefaultEnabled(),
                standard.getRecordType(), standard.getFeedResultSet(), standard.getDefaultFoodName(),
                standard.getEvidenceLevel(), standard.getSourceRefs(), description);
    }
}
