package com.pabom.backend.record.infrastructure.persistence;

import com.pabom.backend.record.domain.entity.Routine;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineJpaRepository extends JpaRepository<Routine, Long> {
    Optional<Routine> findByPetIdAndCareItemIdAndActiveTrue(Long petId, Long careItemId);
    void deleteAllByPetIdIn(Iterable<Long> petIds);
}
