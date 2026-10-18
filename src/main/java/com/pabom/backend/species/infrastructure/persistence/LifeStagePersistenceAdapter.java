package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.repository.LifeStageRepository;
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
}
