package com.pabom.backend.auth.application.command;

public record KakaoCallbackCommand(
        String authorizationCode,
        String expectedState,
        String actualState
) {
}
