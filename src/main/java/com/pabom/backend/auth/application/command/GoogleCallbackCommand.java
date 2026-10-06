package com.pabom.backend.auth.application.command;

public record GoogleCallbackCommand(
        String authorizationCode,
        String expectedState,
        String actualState
) {
}
