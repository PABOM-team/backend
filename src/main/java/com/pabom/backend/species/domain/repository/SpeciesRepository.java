package com.pabom.backend.species.domain.repository;

import com.pabom.backend.species.domain.entity.Species;
import java.util.List;
import java.util.Optional;

public interface SpeciesRepository {

    List<Species> findAllOrdered();

    Optional<Species> findById(Long id);
}
