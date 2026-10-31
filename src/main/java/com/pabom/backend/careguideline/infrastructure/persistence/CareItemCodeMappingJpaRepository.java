package com.pabom.backend.careguideline.infrastructure.persistence;

import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareItemCodeMappingJpaRepository extends JpaRepository<CareItemCodeMapping, Long> {
    Optional<CareItemCodeMapping> findByGuidelineItemCode(String guidelineItemCode);
    Optional<CareItemCodeMapping> findByLegacyCareItemId(Long legacyCareItemId);
    Optional<CareItemCodeMapping> findByGuidelineCareItemId(Long guidelineCareItemId);
}
