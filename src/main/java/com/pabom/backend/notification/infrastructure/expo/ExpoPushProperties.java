package com.pabom.backend.notification.infrastructure.expo;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.push.expo")
public record ExpoPushProperties(
        /* EAS 프로젝트의 "push 보안 강화"용 Access Token. 비어 있으면 발송하지 않는다. */
        String accessToken,
        String baseUrl,
        Integer maxAttempts,
        Duration initialBackoff,
        Duration connectTimeout,
        Duration readTimeout
) {

    public static final String DEFAULT_BASE_URL = "https://exp.host";

    public ExpoPushProperties {
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (maxAttempts == null || maxAttempts < 1) {
            maxAttempts = 3;
        }
        if (initialBackoff == null) {
            initialBackoff = Duration.ofSeconds(1);
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(15);
        }
    }

    public boolean hasAccessToken() {
        return StringUtils.hasText(accessToken);
    }
}
