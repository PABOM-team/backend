package com.pabom.backend.record.infrastructure.persistence;

import com.pabom.backend.record.domain.entity.CareItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareItemJpaRepository extends JpaRepository<CareItem, Long> {
    Optional<CareItem> findByCodeAndActiveTrue(String code);
}
