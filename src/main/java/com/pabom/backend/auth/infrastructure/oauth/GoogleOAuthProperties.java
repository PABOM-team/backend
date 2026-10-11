package com.pabom.backend.auth.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {
}
