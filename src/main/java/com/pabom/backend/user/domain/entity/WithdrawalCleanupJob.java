package com.pabom.backend.user.domain.entity;

import com.pabom.backend.user.domain.type.CleanupJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "withdrawal_cleanup_jobs")
public class WithdrawalCleanupJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CleanupJobStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WithdrawalCleanupJob() {
    }

    private WithdrawalCleanupJob(Long userId, Instant now) {
        this.userId = userId;
        this.status = CleanupJobStatus.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public static WithdrawalCleanupJob pending(Long userId, Instant now) {
        return new WithdrawalCleanupJob(userId, now);
    }

    public void start(Instant now) {
        status = CleanupJobStatus.PROCESSING;
        attempts++;
        nextAttemptAt = now.plusSeconds(10 * 60);
        lastError = null;
    }

    public void fail(Instant now, Throwable failure) {
        status = CleanupJobStatus.FAILED;
        long delaySeconds = Math.min(6 * 60 * 60L, 60L << Math.min(attempts, 8));
        nextAttemptAt = now.plusSeconds(delaySeconds);
        String message = failure.getMessage() == null
                ? failure.getClass().getSimpleName()
                : failure.getMessage();
        lastError = message.substring(0, Math.min(message.length(), 1000));
    }

    public void complete(Instant now) {
        status = CleanupJobStatus.COMPLETED;
        completedAt = now;
        nextAttemptAt = now;
        lastError = null;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public CleanupJobStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
