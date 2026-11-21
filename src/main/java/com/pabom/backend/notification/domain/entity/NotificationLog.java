package com.pabom.backend.notification.domain.entity;

import com.pabom.backend.notification.domain.type.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/** 회원·알림 종류·대상 날짜별 1건만 보내기 위한 발송 기록. UNIQUE 제약이 중복 발송을 막는다. */
@Entity
@Table(
        name = "notification_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_logs_user_type_date",
                columnNames = {"user_id", "notification_type", "target_date"}
        )
)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(Long userId, NotificationType type, LocalDate targetDate, Instant createdAt) {
        this.userId = userId;
        this.type = type;
        this.targetDate = targetDate;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }
}
