package com.pabom.backend.auth.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthWebProperties(
        List<String> allowedOrigins,
        boolean refreshCookieSecure,
        String refreshCookieSameSite
) {

    public AuthWebProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
