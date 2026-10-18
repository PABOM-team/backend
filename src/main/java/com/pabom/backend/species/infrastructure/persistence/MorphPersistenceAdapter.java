package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.entity.Morph;
import com.pabom.backend.species.domain.repository.MorphRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MorphPersistenceAdapter implements MorphRepository {

    private final MorphJpaRepository repository;

    @Override
    public List<Morph> findAllBySpeciesId(Long speciesId) {
        return repository.findAllBySpecies_IdOrderByDisplayOrderAsc(speciesId);
    }

    @Override
    public List<Morph> findAllById(Collection<Long> ids) {
        return repository.findAllById(ids);
    }
}
