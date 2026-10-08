package com.pabom.backend.auth.infrastructure.token;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessExpirationSeconds,
        long signupExpirationSeconds,
        long refreshExpirationSeconds
) {
}
