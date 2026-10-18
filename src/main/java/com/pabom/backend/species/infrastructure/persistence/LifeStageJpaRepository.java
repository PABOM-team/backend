package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.entity.LifeStage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeStageJpaRepository extends JpaRepository<LifeStage, Long> {
}
