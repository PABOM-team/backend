package com.pabom.backend.species.domain.repository;

import com.pabom.backend.species.domain.entity.LifeStage;
import java.util.List;

public interface LifeStageRepository {

    boolean existsById(Long id);

    List<LifeStage> findAllById(Iterable<Long> ids);
}
