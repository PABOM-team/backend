package com.pabom.backend.auth.presentation.mapper;

import com.pabom.backend.auth.application.command.GoogleCallbackCommand;
import com.pabom.backend.auth.application.result.GoogleAuthorizationResult;
import com.pabom.backend.auth.application.result.GoogleLoginResult;
import com.pabom.backend.auth.presentation.response.GoogleAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.GoogleLoginResponse;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthPresentationMapper {

    public GoogleCallbackCommand toCommand(
            String authorizationCode,
            String expectedState,
            String actualState
    ) {
        return new GoogleCallbackCommand(authorizationCode, expectedState, actualState);
    }

    public GoogleAuthorizationUrlResponse toResponse(GoogleAuthorizationResult result) {
        return new GoogleAuthorizationUrlResponse(result.authorizationUrl());
    }

    public GoogleLoginResponse toResponse(GoogleLoginResult result) {
        return new GoogleLoginResponse(
                result.provider().name(),
                result.providerId(),
                result.nickname(),
                result.email()
        );
    }
}
