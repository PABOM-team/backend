package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import com.pabom.backend.careguideline.domain.entity.CareStandard;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.Routine;
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
        boolean unknown = pet.getLifeStageId() == null || !stage.getId().equals(pet.getLifeStageId());

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
            Instant nextDueAt = unknown && "WEIGHT".equals(item.getCode()) ? registeredAt : null;
            created.add(Routine.guideline(pet.getId(), item.getId(), standard.getDefaultCycleDays(),
                    standard.isDefaultEnabled(), version.getId(), nextDueAt, registeredAt));
        }
        routines.saveAll(created);
    }
}

