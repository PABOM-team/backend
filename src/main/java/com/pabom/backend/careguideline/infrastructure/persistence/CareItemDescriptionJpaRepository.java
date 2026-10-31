package com.pabom.backend.careguideline.infrastructure.persistence;

import com.pabom.backend.careguideline.domain.entity.CareItemDescription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareItemDescriptionJpaRepository extends JpaRepository<CareItemDescription, Long> {
    List<CareItemDescription> findAllByGuidelineVersionId(Long guidelineVersionId);
    Optional<CareItemDescription> findByGuidelineVersionIdAndSpeciesIdAndCareItemId(
            Long guidelineVersionId, Long speciesId, Long careItemId);
    Optional<CareItemDescription> findByGuidelineVersionIdAndSpeciesIdIsNullAndCareItemId(
            Long guidelineVersionId, Long careItemId);
}
