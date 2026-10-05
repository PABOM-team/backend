package com.pabom.backend.auth.application.result;

public record KakaoAuthorizationResult(
        String authorizationUrl,
        String state
) {
}
