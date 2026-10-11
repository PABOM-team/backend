package com.pabom.backend.event.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "events")
public class LoginEvent {

    public static final String EVENT_CODE = "EV-02";
    public static final String EVENT_NAME = "login_succeeded";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_code", nullable = false, length = 20)
    private String eventCode;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_new_user", nullable = false)
    private boolean newUser;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LoginEvent() {
    }

    private LoginEvent(Long userId, boolean newUser, Instant occurredAt) {
        this.eventCode = EVENT_CODE;
        this.eventName = EVENT_NAME;
        this.userId = userId;
        this.newUser = newUser;
        this.occurredAt = occurredAt;
    }

    public static LoginEvent succeeded(Long userId, boolean newUser, Instant occurredAt) {
        return new LoginEvent(userId, newUser, occurredAt);
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getEventName() {
        return eventName;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
