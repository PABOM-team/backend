package com.pabom.backend.auth.application.command;

public record OAuthCallbackCommand(
        String authorizationCode,
        String expectedState,
        String actualState
) {
}
