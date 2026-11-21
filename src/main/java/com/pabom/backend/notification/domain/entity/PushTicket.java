package com.pabom.backend.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Expo가 발급한 ticket. 일정 시간 뒤 receipt를 조회해 전달 실패 토큰을 정리한다. */
@Entity
@Table(name = "push_tickets")
public class PushTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false, unique = true, length = 100)
    private String ticketId;

    @Column(nullable = false, length = PushToken.MAX_TOKEN_LENGTH)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PushTicket() {
    }

    public PushTicket(String ticketId, String token, Instant createdAt) {
        this.ticketId = ticketId;
        this.token = token;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getToken() {
        return token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
