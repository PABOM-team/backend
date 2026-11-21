package com.pabom.backend.careguideline.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;

class InitialRoutineCreationServiceTest {
    private final ApplicableCareGuidelineService applicableGuidelines = mock(ApplicableCareGuidelineService.class);
    private final CareItemJpaRepository careItems = mock(CareItemJpaRepository.class);
    private final CareItemCodeMappingJpaRepository mappings = mock(CareItemCodeMappingJpaRepository.class);
    private final RoutineJpaRepository routines = mock(RoutineJpaRepository.class);
    private final CareRecordJpaRepository records = mock(CareRecordJpaRepository.class);
    private final RoutineDueDateCalculator calculator = mock(RoutineDueDateCalculator.class);
    private final InitialRoutineCreationService service = new InitialRoutineCreationService(
            applicableGuidelines, careItems, mappings, routines, records, calculator);

    @Test
    void keepsExistingBehaviorWhenNoApplicableGuidelineExists() {
        when(applicableGuidelines.findApplicable()).thenReturn(Optional.empty());

        service.createIfApplicable(mock(Pet.class), Instant.parse("2026-11-01T03:00:00Z"));

        verifyNoInteractions(careItems, mappings, routines, calculator);
    }

    @Test
    void rejectsLifeStageSynchronizationWhenExistingRoutinesHaveNoApplicableGuideline() {
        Pet pet = mock(Pet.class);
        when(pet.getId()).thenReturn(1L);
        when(routines.findAllByPetIdOrderByIdAsc(1L)).thenReturn(List.of(mock(Routine.class)));
        when(applicableGuidelines.findApplicable()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.synchronizeForLifeStageChange(
                pet, Instant.parse("2026-11-01T03:00:00Z")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(
                                CareGuidelineQueryErrorCode.APPLICABLE_GUIDELINE_NOT_FOUND));
    }
}
