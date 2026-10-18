package com.pabom.backend.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.species.domain.error.SpeciesErrorCode;
import com.pabom.backend.species.domain.repository.LifeStageRepository;
import com.pabom.backend.species.domain.repository.MorphRepository;
import com.pabom.backend.species.domain.repository.SpeciesRepository;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetValidatorTest {

    private LifeStageRepository lifeStageRepository;
    private PetValidator validator;

    @BeforeEach
    void setUp() {
        lifeStageRepository = mock(LifeStageRepository.class);
        validator = new PetValidator(
                mock(SpeciesRepository.class),
                mock(MorphRepository.class),
                lifeStageRepository,
                mock(FileRepository.class),
                Clock.systemUTC()
        );
    }

    @Test
    void requiresLifeStageForConfirmedOrEstimatedCertainty() {
        assertError(null, Certainty.CONFIRMED, PetErrorCode.STAGE_REQUIRED);
        assertError(null, Certainty.ESTIMATED, PetErrorCode.STAGE_REQUIRED);
    }

    @Test
    void allowsOnlyNullLifeStageForUnknownCertainty() {
        assertThatCode(() -> validator.validateLifeStage(null, Certainty.UNKNOWN))
                .doesNotThrowAnyException();
        assertError(1L, Certainty.UNKNOWN, SpeciesErrorCode.LIFE_STAGE_INVALID);
    }

    @Test
    void acceptsExistingLifeStageForConfirmedOrEstimatedCertainty() {
        given(lifeStageRepository.existsById(1L)).willReturn(true);

        assertThatCode(() -> validator.validateLifeStage(1L, Certainty.CONFIRMED))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateLifeStage(1L, Certainty.ESTIMATED))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonexistentLifeStage() {
        given(lifeStageRepository.existsById(999L)).willReturn(false);

        assertError(999L, Certainty.CONFIRMED, SpeciesErrorCode.LIFE_STAGE_INVALID);
    }

    private void assertError(Long lifeStageId, Certainty certainty, Object errorCode) {
        assertThatThrownBy(() -> validator.validateLifeStage(lifeStageId, certainty))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(errorCode));
    }
}
