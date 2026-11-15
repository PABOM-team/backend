package com.pabom.backend.auth.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 앱 로그인 후 돌아갈 수 있는 앱 주소 허용 목록.
 * {provider}는 kakao, google 등 소문자 Provider 이름으로 치환된다.
 */
@ConfigurationProperties(prefix = "app.auth.app-redirect")
public record AuthAppRedirectProperties(
        List<String> allowedUris
) {

    public AuthAppRedirectProperties {
        allowedUris = allowedUris == null ? List.of() : List.copyOf(allowedUris);
    }
}
