package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import com.pabom.backend.careguideline.domain.entity.CareStandard;
import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.CareRecord;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.domain.type.CycleSource;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.species.domain.entity.LifeStage;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitialRoutineCreationService {
    private final ApplicableCareGuidelineService applicableGuidelines;
    private final CareItemJpaRepository careItems;
    private final CareItemCodeMappingJpaRepository mappings;
    private final RoutineJpaRepository routines;
    private final CareRecordJpaRepository records;
    private final RoutineDueDateCalculator dueDateCalculator;

    public void createIfApplicable(Pet pet, Instant registeredAt) {
        Optional<CareGuidelineVersion> applicable = applicableGuidelines.findApplicable();
        if (applicable.isEmpty()) return;
        CareGuidelineVersion version = applicable.get();
        LifeStage stage = applicableGuidelines.guidelineStage(pet.getLifeStageId());
        List<CareStandard> standards = applicableGuidelines.standards(
                version.getId(), pet.getSpeciesId(), stage.getId());
        if (standards.isEmpty()) return;

        Map<Long, CareItem> items = new HashMap<>();
        careItems.findAllById(standards.stream().map(CareStandard::getCareItemId).toList())
                .forEach(item -> items.put(item.getId(), item));
        Map<Long, CareItemCodeMapping> mappingByGuidelineItem = new HashMap<>();
        mappings.findAll().stream().filter(mapping -> mapping.getGuidelineCareItemId() != null)
                .forEach(mapping -> mappingByGuidelineItem.put(mapping.getGuidelineCareItemId(), mapping));
        List<CareStandard> ordered = standards.stream().sorted(Comparator
                .comparing((CareStandard standard) -> !"WEIGHT".equals(items.get(standard.getCareItemId()).getCode()))
                .thenComparing(standard -> items.get(standard.getCareItemId()).getCode())).toList();
        List<Routine> created = new ArrayList<>();
        for (CareStandard standard : ordered) {
            CareItem item = items.get(standard.getCareItemId());
            if (item == null) continue;
            Set<Long> compatibleItemIds = new HashSet<>();
            compatibleItemIds.add(item.getId());
            CareItemCodeMapping mapping = mappingByGuidelineItem.get(item.getId());
            if (mapping != null) compatibleItemIds.add(mapping.getLegacyCareItemId());
            if (routines.existsByPetIdAndCareItemIdIn(pet.getId(), compatibleItemIds)) continue;
            Instant nextDueAt = dueDateCalculator.firstDueAt(item.getCode(),
                    standard.isDefaultEnabled(), standard.getDefaultCycleDays(), registeredAt);
            java.time.LocalDate startDate = nextDueAt == null ? null
                    : dueDateCalculator.toServiceDate(nextDueAt);
            created.add(Routine.guideline(pet.getId(), item.getId(), standard.getDefaultCycleDays(),
                    standard.isDefaultEnabled(), version.getId(), startDate, nextDueAt, registeredAt));
        }
        routines.saveAll(created);
    }

    public void synchronizeForLifeStageChange(Pet pet, Instant changedAt) {
        List<Routine> existing = routines.findAllByPetIdOrderByIdAsc(pet.getId());
        Optional<CareGuidelineVersion> applicable = applicableGuidelines.findApplicable();
        if (applicable.isEmpty()) {
            if (existing.isEmpty()) return;
            throw new BusinessException(CareGuidelineQueryErrorCode.APPLICABLE_GUIDELINE_NOT_FOUND);
        }

        CareGuidelineVersion version = applicable.orElseThrow();
        LifeStage stage = applicableGuidelines.guidelineStage(pet.getLifeStageId());
        List<CareStandard> standards = applicableGuidelines.standards(
                version.getId(), pet.getSpeciesId(), stage.getId());
        if (standards.isEmpty()) {
            throw new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING);
        }

        Map<Long, CareItem> items = new HashMap<>();
        careItems.findAllById(standards.stream().map(CareStandard::getCareItemId).toList())
                .forEach(item -> items.put(item.getId(), item));
        List<CareItemCodeMapping> allMappings = mappings.findAll();
        Map<Long, CareItemCodeMapping> byLegacy = allMappings.stream()
                .collect(java.util.stream.Collectors.toMap(
                        CareItemCodeMapping::getLegacyCareItemId, mapping -> mapping));
        Map<Long, CareItemCodeMapping> byGuideline = allMappings.stream()
                .filter(mapping -> mapping.getGuidelineCareItemId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        CareItemCodeMapping::getGuidelineCareItemId, mapping -> mapping));

        Map<Long, Routine> existingByCanonicalItem = new LinkedHashMap<>();
        for (Routine routine : existing) {
            CareItemCodeMapping mapping = byLegacy.get(routine.getCareItemId());
            Long canonicalItemId = mapping != null && mapping.getGuidelineCareItemId() != null
                    ? mapping.getGuidelineCareItemId() : routine.getCareItemId();
            Routine previous = existingByCanonicalItem.get(canonicalItemId);
            if (previous == null || routine.getCareItemId().equals(canonicalItemId)) {
                existingByCanonicalItem.put(canonicalItemId, routine);
            }
        }

        List<Routine> changed = new ArrayList<>();
        for (CareStandard standard : standards) {
            CareItem item = items.get(standard.getCareItemId());
            if (item == null) continue;
            Routine routine = existingByCanonicalItem.get(standard.getCareItemId());
            if (routine == null) {
                Set<Long> compatibleItemIds = new HashSet<>();
                compatibleItemIds.add(item.getId());
                CareItemCodeMapping mapping = byGuideline.get(item.getId());
                if (mapping != null) compatibleItemIds.add(mapping.getLegacyCareItemId());
                if (routines.existsByPetIdAndCareItemIdIn(pet.getId(), compatibleItemIds)) continue;
                Instant nextDueAt = dueDateCalculator.firstDueAt(item.getCode(),
                        standard.isDefaultEnabled(), standard.getDefaultCycleDays(), changedAt);
                java.time.LocalDate startDate = nextDueAt == null ? null
                        : dueDateCalculator.toServiceDate(nextDueAt);
                changed.add(Routine.guideline(pet.getId(), item.getId(), standard.getDefaultCycleDays(),
                        standard.isDefaultEnabled(), version.getId(), startDate, nextDueAt, changedAt));
                continue;
            }

            if (routine.getCycleSource() != CycleSource.GUIDELINE) {
                if (!Objects.equals(routine.getGuidelineVersionId(), version.getId())) {
                    routine.synchronizeGuidelineVersion(version.getId(), changedAt);
                    changed.add(routine);
                }
                continue;
            }

            boolean intervalChanged = !Objects.equals(
                    routine.getIntervalDays(), standard.getDefaultCycleDays());
            java.time.LocalDate startDate = routine.getStartDate();
            Instant nextDueAt = routine.getNextDueAt();
            if (intervalChanged && routine.getPostponedBaseDue() == null) {
                Optional<CareRecord> lastRecord = records
                        .findFirstByUserIdAndRoutineIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(
                                pet.getUserId(), routine.getId());
                if (lastRecord.isPresent()) {
                    nextDueAt = dueDateCalculator.nextDueAfterCompletion(
                            dueDateCalculator.toServiceDate(lastRecord.get().getOccurredAt()),
                            standard.getDefaultCycleDays());
                } else if (routine.isStartDateCustom() && startDate != null) {
                    nextDueAt = dueDateCalculator.toDateOnlyInstant(startDate);
                } else {
                    nextDueAt = dueDateCalculator.firstDueAt(item.getCode(), routine.isActive(),
                            standard.getDefaultCycleDays(), pet.getCreatedAt());
                    startDate = nextDueAt == null ? null : dueDateCalculator.toServiceDate(nextDueAt);
                }
            }
            if (intervalChanged || !Objects.equals(routine.getGuidelineVersionId(), version.getId())) {
                routine.synchronizeGuideline(standard.getDefaultCycleDays(), version.getId(),
                        startDate, nextDueAt, changedAt);
                changed.add(routine);
            }
        }
        routines.saveAll(changed);
    }
}

