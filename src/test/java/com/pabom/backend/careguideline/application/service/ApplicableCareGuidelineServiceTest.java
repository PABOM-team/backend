package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.domain.type.GuidelineVersionStatus;
import com.pabom.backend.careguideline.infrastructure.persistence.CareGuidelineVersionJpaRepository;
import com.pabom.backend.careguideline.infrastructure.persistence.CareStandardJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.species.infrastructure.persistence.LifeStageJpaRepository;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicableCareGuidelineServiceTest {
    @Test
    void reportsNoApplicableVersionBeforeEffectiveDateUsingSeoulDate() {
        CareGuidelineVersionJpaRepository versions = mock(CareGuidelineVersionJpaRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-31T14:59:59Z"), ZoneOffset.UTC);
        given(versions.findAllByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                GuidelineVersionStatus.PUBLISHED, LocalDate.of(2026, 10, 31))).willReturn(List.of());
        ApplicableCareGuidelineService service = new ApplicableCareGuidelineService(
                versions, mock(CareStandardJpaRepository.class), mock(LifeStageJpaRepository.class), clock);

        assertThatThrownBy(service::requireApplicable)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(
                                CareGuidelineQueryErrorCode.APPLICABLE_GUIDELINE_NOT_FOUND));
        verify(versions).findAllByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                GuidelineVersionStatus.PUBLISHED, LocalDate.of(2026, 10, 31));
    }
}
