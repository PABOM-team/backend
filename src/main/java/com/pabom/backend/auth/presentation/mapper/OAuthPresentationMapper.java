package com.pabom.backend.auth.presentation.mapper;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.presentation.response.OAuthAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.OAuthLoginResponse;
import org.springframework.stereotype.Component;

@Component
public class OAuthPresentationMapper {

    public OAuthCallbackCommand toCommand(
            String authorizationCode,
            String expectedState,
            String actualState
    ) {
        return new OAuthCallbackCommand(authorizationCode, expectedState, actualState);
    }

    public OAuthAuthorizationUrlResponse toResponse(OAuthAuthorizationResult result) {
        return new OAuthAuthorizationUrlResponse(result.authorizationUrl());
    }

    public OAuthLoginResponse toResponse(OAuthLoginResult result) {
        return new OAuthLoginResponse(
                result.provider().name(),
                result.providerId(),
                result.nickname()
        );
    }
}
