package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.species.infrastructure.persistence.SpeciesJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class TodayQueryServiceTest {
    @Test
    void ignoresRequestedDateOutsideDevelopmentProfilesAndUsesSeoulDate() {
        PetRepository pets = mock(PetRepository.class);
        Environment environment = mock(Environment.class);
        when(pets.findAllByUserId(1L)).thenReturn(List.of());
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        TodayQueryService service = new TodayQueryService(pets, mock(RoutineJpaRepository.class),
                mock(CareRecordJpaRepository.class), mock(CareItemJpaRepository.class),
                mock(CareItemCodeMappingJpaRepository.class), mock(SpeciesJpaRepository.class),
                new RoutineDueDateCalculator(),
                Clock.fixed(Instant.parse("2026-11-02T15:30:00Z"), ZoneOffset.UTC), environment);

        assertThat(service.get(1L, null, LocalDate.of(2020, 1, 1)).date())
                .isEqualTo(LocalDate.of(2026, 11, 3));
    }
}
