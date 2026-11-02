package com.pabom.backend.careguideline.infrastructure.persistence;

import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import com.pabom.backend.careguideline.domain.type.GuidelineVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareGuidelineVersionJpaRepository extends JpaRepository<CareGuidelineVersion, Long> {
    boolean existsByVersionLabel(String versionLabel);
    Optional<CareGuidelineVersion> findByVersionLabel(String versionLabel);
    Optional<CareGuidelineVersion> findFirstByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            GuidelineVersionStatus status, LocalDate effectiveFrom);
    List<CareGuidelineVersion> findAllByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            GuidelineVersionStatus status, LocalDate effectiveFrom);
}
