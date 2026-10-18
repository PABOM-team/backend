package com.pabom.backend.species.infrastructure.persistence;

import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.domain.type.ReptileCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesJpaRepository extends JpaRepository<Species, Long> {

    List<Species> findAllByOrderByCategoryAscDisplayOrderAsc();

    List<Species> findAllByCategoryOrderByDisplayOrderAsc(ReptileCategory category);
}
