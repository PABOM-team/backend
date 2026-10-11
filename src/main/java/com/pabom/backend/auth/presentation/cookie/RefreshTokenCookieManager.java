package com.pabom.backend.auth.presentation.cookie;

import com.pabom.backend.auth.infrastructure.config.AuthWebProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieManager {

    public static final String COOKIE_NAME = "pabom_rt";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieManager(AuthWebProperties properties) {
        this.secure = properties.refreshCookieSecure();
        this.sameSite = properties.refreshCookieSameSite();
    }

    public void add(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        addCookie(response, refreshToken, Duration.ofSeconds(maxAgeSeconds));
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
