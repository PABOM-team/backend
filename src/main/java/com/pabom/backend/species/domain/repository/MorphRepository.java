package com.pabom.backend.species.domain.repository;

import com.pabom.backend.species.domain.entity.Morph;
import java.util.Collection;
import java.util.List;

public interface MorphRepository {

    List<Morph> findAllBySpeciesId(Long speciesId);

    List<Morph> findAllById(Collection<Long> ids);
}
