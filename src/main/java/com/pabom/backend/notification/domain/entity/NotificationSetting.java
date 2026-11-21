package com.pabom.backend.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@Entity
@Table(name = "notification_settings")
public class NotificationSetting {

    public static final boolean DEFAULT_CARE_REMINDER_ENABLED = true;
    public static final LocalTime DEFAULT_CARE_REMINDER_TIME = LocalTime.of(9, 0);
    public static final String DEFAULT_TIME_ZONE = "Asia/Seoul";
    /** 케어 알림 시각 단위(분). 발송 스케줄러 주기와 같아야 한다. */
    public static final int REMINDER_SLOT_MINUTES = 30;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "care_reminder_enabled", nullable = false)
    private boolean careReminderEnabled;

    @Column(name = "care_reminder_time", nullable = false)
    private LocalTime careReminderTime;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationSetting() {
    }

    private NotificationSetting(Long userId, Instant now) {
        this.userId = userId;
        this.careReminderEnabled = DEFAULT_CARE_REMINDER_ENABLED;
        this.careReminderTime = DEFAULT_CARE_REMINDER_TIME;
        this.timeZone = DEFAULT_TIME_ZONE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 설정 행이 없는 회원의 기본값 (켜짐, 09:00, Asia/Seoul). */
    public static NotificationSetting defaults(Long userId, Instant now) {
        return new NotificationSetting(userId, now);
    }

    public static boolean isValidReminderTime(LocalTime time) {
        return time != null
                && time.getSecond() == 0
                && time.getNano() == 0
                && time.getMinute() % REMINDER_SLOT_MINUTES == 0;
    }

    public static boolean isValidTimeZone(String timeZone) {
        return timeZone != null && ZoneId.getAvailableZoneIds().contains(timeZone);
    }

    public void update(Boolean enabled, LocalTime reminderTime, String timeZone, Instant now) {
        if (enabled != null) {
            this.careReminderEnabled = enabled;
        }
        if (reminderTime != null) {
            this.careReminderTime = reminderTime;
        }
        if (timeZone != null) {
            this.timeZone = timeZone;
        }
        this.updatedAt = now;
    }

    public ZoneId zoneId() {
        return ZoneId.of(timeZone);
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isCareReminderEnabled() {
        return careReminderEnabled;
    }

    public LocalTime getCareReminderTime() {
        return careReminderTime;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
