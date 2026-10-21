package com.pabom.backend.user.application.service;

import com.pabom.backend.agreement.domain.repository.UserAgreementRepository;
import com.pabom.backend.event.domain.repository.LoginEventRepository;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordRevisionJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.user.domain.entity.WithdrawalCleanupJob;
import com.pabom.backend.user.domain.repository.RefreshTokenRepository;
import com.pabom.backend.user.domain.type.CleanupJobStatus;
import com.pabom.backend.user.infrastructure.persistence.WithdrawalCleanupJobJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalCleanupService {

    private static final int BATCH_SIZE = 50;

    private final WithdrawalCleanupJobJpaRepository jobRepository;
    private final FileRepository fileRepository;
    private final FileStoragePort fileStorage;
    private final PetRepository petRepository;
    private final CareRecordRevisionJpaRepository recordRevisionRepository;
    private final CareRecordJpaRepository recordRepository;
    private final RoutineJpaRepository routineRepository;
    private final UserAgreementRepository agreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginEventRepository loginEventRepository;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public void processDueJobs() {
        Instant now = clock.instant();
        List<Long> jobIds = jobRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAt(
                        List.of(CleanupJobStatus.PENDING, CleanupJobStatus.FAILED,
                                CleanupJobStatus.PROCESSING),
                        now,
                        PageRequest.of(0, BATCH_SIZE)
                ).stream().map(WithdrawalCleanupJob::getId).toList();
        jobIds.forEach(this::process);
    }

    public void process(Long jobId) {
        try {
            Long userId = transactions.execute(status -> {
                WithdrawalCleanupJob job = jobRepository.findByIdForUpdate(jobId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Cleanup job not found: " + jobId));
                Instant now = clock.instant();
                if (job.getStatus() == CleanupJobStatus.COMPLETED
                        || (job.getStatus() == CleanupJobStatus.PROCESSING
                        && job.getNextAttemptAt().isAfter(now))) {
                    return null;
                }
                job.start(now);
                return job.getUserId();
            });
            if (userId == null) {
                return;
            }

            List<String> storageKeys = transactions.execute(status ->
                    fileRepository.findAllByOwnerId(userId).stream()
                            .map(com.pabom.backend.file.domain.entity.File::getStorageKey)
                            .toList());
            storageKeys.forEach(fileStorage::delete);

            transactions.executeWithoutResult(status -> {
                // Withdrawal keeps its 24-hour policy: it removes active and soft-deleted pets immediately,
                // independently of the pet cleanup scheduler's 30-day retention period.
                List<Long> petIds = petRepository.findAllIncludingDeletedByUserId(userId).stream()
                        .map(com.pabom.backend.pet.domain.entity.Pet::getId).toList();
                recordRevisionRepository.deleteAllByUserId(userId);
                recordRepository.deleteAllByUserId(userId);
                routineRepository.deleteAllByPetIdIn(petIds);
                petRepository.deleteAllByUserId(userId);
                fileRepository.deleteAllByOwnerId(userId);
                agreementRepository.deleteAllByUserId(userId);
                refreshTokenRepository.deleteAllByUserId(userId);
                loginEventRepository.anonymizeByUserId(userId);
                requiredJob(jobId).complete(clock.instant());
            });
        } catch (RuntimeException failure) {
            log.warn("Withdrawal cleanup failed. jobId={}", jobId, failure);
            transactions.executeWithoutResult(status -> {
                WithdrawalCleanupJob job = requiredJob(jobId);
                if (job.getStatus() != CleanupJobStatus.COMPLETED) {
                    job.fail(clock.instant(), failure);
                }
                if (Duration.between(job.getCreatedAt(), clock.instant()).toHours() >= 20) {
                    log.error("Withdrawal cleanup is approaching its 24-hour deadline. jobId={}", jobId);
                }
            });
        }
    }

    private WithdrawalCleanupJob requiredJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Cleanup job not found: " + jobId));
    }
}
