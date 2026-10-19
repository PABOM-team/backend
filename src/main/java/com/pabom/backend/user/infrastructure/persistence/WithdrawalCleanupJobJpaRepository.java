package com.pabom.backend.user.infrastructure.persistence;

import com.pabom.backend.user.domain.entity.WithdrawalCleanupJob;
import com.pabom.backend.user.domain.type.CleanupJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WithdrawalCleanupJobJpaRepository
        extends JpaRepository<WithdrawalCleanupJob, Long> {

    List<WithdrawalCleanupJob> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAt(
            Collection<CleanupJobStatus> statuses,
            Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from WithdrawalCleanupJob job where job.id = :id")
    Optional<WithdrawalCleanupJob> findByIdForUpdate(@Param("id") Long id);
}
