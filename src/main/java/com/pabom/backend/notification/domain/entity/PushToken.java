package com.pabom.backend.notification.domain.entity;

import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.regex.Pattern;

@Entity
@Table(name = "push_tokens")
public class PushToken {

    /** Expo 푸시 토큰: ExponentPushToken[...] 또는 ExpoPushToken[...] */
    private static final Pattern EXPO_TOKEN_PATTERN =
            Pattern.compile("^(ExponentPushToken|ExpoPushToken)\\[[^\\[\\]\\s]+]$");
    public static final int MAX_TOKEN_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = MAX_TOKEN_LENGTH)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_variant", nullable = false, length = 20)
    private AppVariant appVariant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected PushToken() {
    }

    public static boolean isValidFormat(String token) {
        return token != null
                && token.length() <= MAX_TOKEN_LENGTH
                && EXPO_TOKEN_PATTERN.matcher(token).matches();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public PushPlatform getPlatform() {
        return platform;
    }

    public AppVariant getAppVariant() {
        return appVariant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
