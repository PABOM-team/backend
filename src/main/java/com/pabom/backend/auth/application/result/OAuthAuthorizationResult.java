package com.pabom.backend.auth.application.result;

public record OAuthAuthorizationResult(
        String authorizationUrl,
        String state
) {
}
