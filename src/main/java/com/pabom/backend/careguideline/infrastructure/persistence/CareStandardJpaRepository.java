package com.pabom.backend.careguideline.infrastructure.persistence;

import com.pabom.backend.careguideline.domain.entity.CareStandard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareStandardJpaRepository extends JpaRepository<CareStandard, Long> {
    List<CareStandard> findAllByGuidelineVersionId(Long guidelineVersionId);
    List<CareStandard> findAllByGuidelineVersionIdAndSpeciesIdAndLifeStageId(
            Long guidelineVersionId, Long speciesId, Long lifeStageId);
}
