package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.domain.repository.SpeciesRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SpeciesPersistenceAdapter implements SpeciesRepository {

    private final SpeciesJpaRepository repository;

    @Override
    public List<Species> findAllOrdered() {
        return repository.findAllByOrderByCategoryAscDisplayOrderAsc();
    }

    @Override
    public Optional<Species> findById(Long id) {
        return repository.findById(id);
    }
}
