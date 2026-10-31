package com.pabom.backend.careguideline.infrastructure.persistence;

import com.pabom.backend.careguideline.domain.entity.CareStageThreshold;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareStageThresholdJpaRepository extends JpaRepository<CareStageThreshold, Long> {
    List<CareStageThreshold> findAllByGuidelineVersionId(Long guidelineVersionId);
    Optional<CareStageThreshold> findByGuidelineVersionIdAndSpeciesIdAndSex(
            Long guidelineVersionId, Long speciesId, String sex);
}
