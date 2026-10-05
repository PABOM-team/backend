package com.pabom.backend.auth.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kakao")
public record KakaoOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {
}
