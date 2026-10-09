package com.pabom.backend.auth.presentation.response;

public record TokenRefreshResponse(
        String accessToken,
        long expiresIn
) {
}
