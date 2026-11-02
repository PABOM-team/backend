package com.pabom.backend.record.infrastructure.persistence;

import com.pabom.backend.record.domain.entity.RoutinePostponementEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutinePostponementEventJpaRepository
        extends JpaRepository<RoutinePostponementEvent, Long> {
    List<RoutinePostponementEvent> findAllByRoutineIdOrderByIdAsc(Long routineId);
    Optional<RoutinePostponementEvent> findFirstByRoutineIdOrderByIdDesc(Long routineId);
}
