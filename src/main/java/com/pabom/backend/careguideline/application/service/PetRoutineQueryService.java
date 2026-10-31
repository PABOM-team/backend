package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.application.result.PetRoutineResult;
import com.pabom.backend.careguideline.domain.entity.*;
import com.pabom.backend.careguideline.infrastructure.persistence.*;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.domain.type.CycleSource;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.species.domain.entity.LifeStage;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetRoutineQueryService {
    private final PetRepository pets;
    private final RoutineJpaRepository routines;
    private final CareItemJpaRepository careItems;
    private final CareItemCodeMappingJpaRepository mappings;
    private final CareGuidelineVersionJpaRepository versions;
    private final CareStandardJpaRepository standards;
    private final CareItemDescriptionJpaRepository descriptions;
    private final ApplicableCareGuidelineService applicableGuidelines;

    public List<PetRoutineResult> get(Long userId, Long petId) {
        Pet pet = pets.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        List<Routine> all = routines.findAllByPetIdOrderByIdAsc(petId);
        Map<Long, CareItem> itemById = careItems.findAllById(
                        all.stream().map(Routine::getCareItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(CareItem::getId, Function.identity()));
        List<CareItemCodeMapping> mappingList = mappings.findAll();
        Map<Long, CareItemCodeMapping> byLegacy = mappingList.stream()
                .collect(Collectors.toMap(CareItemCodeMapping::getLegacyCareItemId, Function.identity()));
        Map<Long, CareItemCodeMapping> byGuideline = mappingList.stream()
                .filter(mapping -> mapping.getGuidelineCareItemId() != null)
                .collect(Collectors.toMap(CareItemCodeMapping::getGuidelineCareItemId, Function.identity()));

        Set<Long> canonicalItemIds = all.stream().map(routine -> canonicalItemId(routine, byLegacy)).collect(Collectors.toSet());
        careItems.findAllById(canonicalItemIds).forEach(item -> itemById.put(item.getId(), item));
        LinkedHashMap<Long, Routine> deduplicated = new LinkedHashMap<>();
        for (Routine routine : all) {
            long key = canonicalItemId(routine, byLegacy);
            Routine existing = deduplicated.get(key);
            if (existing == null || isGuidelineItem(routine, byGuideline)) deduplicated.put(key, routine);
        }

        Optional<CareGuidelineVersion> current = applicableGuidelines.findApplicable();
        LifeStage stage = applicableGuidelines.guidelineStage(pet.getLifeStageId());
        Set<Long> versionIds = deduplicated.values().stream().map(Routine::getGuidelineVersionId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        current.map(CareGuidelineVersion::getId).ifPresent(versionIds::add);
        Map<Long, CareGuidelineVersion> versionById = versions.findAllById(versionIds).stream()
                .collect(Collectors.toMap(CareGuidelineVersion::getId, Function.identity()));

        return deduplicated.entrySet().stream().map(entry -> {
            Routine routine = entry.getValue();
            Long canonicalItemId = entry.getKey();
            CareItem item = itemById.get(canonicalItemId);
            Long versionId = routine.getGuidelineVersionId() != null ? routine.getGuidelineVersionId()
                    : current.map(CareGuidelineVersion::getId).orElse(null);
            CareStandard standard = versionId == null ? null : standards
                    .findByGuidelineVersionIdAndSpeciesIdAndLifeStageIdAndCareItemId(
                            versionId, pet.getSpeciesId(), stage.getId(), canonicalItemId).orElse(null);
            String description = versionId == null ? null : descriptions
                    .findByGuidelineVersionIdAndSpeciesIdAndCareItemId(versionId, pet.getSpeciesId(), canonicalItemId)
                    .or(() -> descriptions.findByGuidelineVersionIdAndSpeciesIdIsNullAndCareItemId(versionId, canonicalItemId))
                    .map(CareItemDescription::getDescriptionText).orElse(null);
            Integer interval = routine.getIntervalDays();
            Boolean outOfRange = standard == null || interval == null ? null
                    : interval < standard.getMinDays() || interval > standard.getMaxDays();
            String code = item == null ? null : item.getCode();
            String name = item == null ? null : item.getName();
            if (byLegacy.containsKey(routine.getCareItemId())) {
                code = byLegacy.get(routine.getCareItemId()).getGuidelineItemCode();
            }
            CareGuidelineVersion version = versionId == null ? null : versionById.get(versionId);
            return new PetRoutineResult(routine.getId(), petId, code, name, routine.isActive(), interval,
                    routine.getCycleSource() == null ? CycleSource.USER_CUSTOM : routine.getCycleSource(),
                    standard == null ? null : standard.getDefaultCycleDays(),
                    standard == null ? null : standard.getMinDays(),
                    standard == null ? null : standard.getMaxDays(), outOfRange,
                    routine.getNextDueAt() == null ? null : OffsetDateTime.ofInstant(
                            routine.getNextDueAt(), ApplicableCareGuidelineService.SERVICE_ZONE),
                    version == null ? null : version.getVersionLabel(),
                    standard == null ? item == null ? null : item.getGuidelineRecordType() : standard.getRecordType(),
                    description);
        }).toList();
    }

    private Long canonicalItemId(Routine routine, Map<Long, CareItemCodeMapping> byLegacy) {
        CareItemCodeMapping mapping = byLegacy.get(routine.getCareItemId());
        return mapping != null && mapping.getGuidelineCareItemId() != null
                ? mapping.getGuidelineCareItemId() : routine.getCareItemId();
    }

    private boolean isGuidelineItem(Routine routine, Map<Long, CareItemCodeMapping> byGuideline) {
        return byGuideline.containsKey(routine.getCareItemId());
    }
}
