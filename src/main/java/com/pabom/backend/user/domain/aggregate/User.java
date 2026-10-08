package com.pabom.backend.user.domain.aggregate;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.user.domain.type.UserStatus;
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

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_provider_provider_id",
                columnNames = {"provider", "provider_id"}
        )
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 191)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    private User(
            OAuthProvider provider,
            String providerId,
            String nickname,
            Instant loginAt
    ) {
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.status = UserStatus.PENDING_TERMS;
        this.lastLoginAt = loginAt;
        this.createdAt = loginAt;
        this.updatedAt = loginAt;
    }

    public static User pending(
            OAuthProvider provider,
            String providerId,
            String nickname,
            Instant loginAt
    ) {
        return new User(provider, providerId, nickname, loginAt);
    }

    public void recordLogin(Instant loginAt) {
        this.lastLoginAt = loginAt;
        this.updatedAt = loginAt;
    }

    public void activate(Instant activatedAt) {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = activatedAt;
    }

    public Long getId() {
        return id;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getNickname() {
        return nickname;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
