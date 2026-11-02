package com.pabom.backend.careguideline.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InitialRoutineCreationServiceTest {
    private final ApplicableCareGuidelineService applicableGuidelines = mock(ApplicableCareGuidelineService.class);
    private final CareItemJpaRepository careItems = mock(CareItemJpaRepository.class);
    private final CareItemCodeMappingJpaRepository mappings = mock(CareItemCodeMappingJpaRepository.class);
    private final RoutineJpaRepository routines = mock(RoutineJpaRepository.class);
    private final RoutineDueDateCalculator calculator = mock(RoutineDueDateCalculator.class);
    private final InitialRoutineCreationService service = new InitialRoutineCreationService(
            applicableGuidelines, careItems, mappings, routines, calculator);

    @Test
    void keepsExistingBehaviorWhenNoApplicableGuidelineExists() {
        when(applicableGuidelines.findApplicable()).thenReturn(Optional.empty());

        service.createIfApplicable(mock(Pet.class), Instant.parse("2026-11-01T03:00:00Z"));

        verifyNoInteractions(careItems, mappings, routines, calculator);
    }
}
