package com.pabom.backend.auth.application.result;

public record GoogleAuthorizationResult(
        String authorizationUrl,
        String state
) {
}
