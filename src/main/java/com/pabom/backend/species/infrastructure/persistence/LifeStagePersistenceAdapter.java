package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.entity.LifeStage;
import com.pabom.backend.species.domain.repository.LifeStageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LifeStagePersistenceAdapter implements LifeStageRepository {

    private final LifeStageJpaRepository repository;

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public List<LifeStage> findAllById(Iterable<Long> ids) {
        return repository.findAllById(ids);
    }
}
