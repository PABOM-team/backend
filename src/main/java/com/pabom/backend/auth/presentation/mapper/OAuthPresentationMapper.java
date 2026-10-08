package com.pabom.backend.auth.presentation.mapper;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginCompletionResult;
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

    public OAuthLoginResponse toResponse(OAuthLoginCompletionResult result) {
        OAuthLoginResponse.UserInfo user = new OAuthLoginResponse.UserInfo(
                result.user().getId(),
                result.user().getNickname(),
                result.user().getStatus()
        );
        return switch (result.tokenType()) {
            case SIGNUP -> new OAuthLoginResponse.Signup(
                    result.token(),
                    result.expiresIn(),
                    result.isNewUser(),
                    user
            );
            case ACCESS -> new OAuthLoginResponse.Access(
                    result.token(),
                    result.expiresIn(),
                    result.isNewUser(),
                    user
            );
        };
    }
}
