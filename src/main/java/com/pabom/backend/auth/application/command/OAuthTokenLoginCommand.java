package com.pabom.backend.auth.application.command;

public record OAuthTokenLoginCommand(
        String accessToken,
        String idToken
) {
}
