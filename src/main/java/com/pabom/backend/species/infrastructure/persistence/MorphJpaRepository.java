package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.entity.Morph;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MorphJpaRepository extends JpaRepository<Morph, Long> {

    List<Morph> findAllBySpecies_IdOrderByDisplayOrderAsc(Long speciesId);
}
