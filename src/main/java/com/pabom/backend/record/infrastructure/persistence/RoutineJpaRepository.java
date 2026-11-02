package com.pabom.backend.record.infrastructure.persistence;

import com.pabom.backend.record.domain.entity.Routine;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineJpaRepository extends JpaRepository<Routine, Long> {
    Optional<Routine> findByPetIdAndCareItemIdAndActiveTrue(Long petId, Long careItemId);
    List<Routine> findAllByPetIdOrderByIdAsc(Long petId);
    List<Routine> findAllByPetIdInAndActiveTrue(Collection<Long> petIds);
    boolean existsByPetIdAndCareItemIdIn(Long petId, Collection<Long> careItemIds);
    void deleteAllByPetIdIn(Iterable<Long> petIds);
    void deleteAllByPetId(Long petId);
}
