package com.pabom.backend.auth.application.result;

public record TokenRefreshResult(
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {
}
