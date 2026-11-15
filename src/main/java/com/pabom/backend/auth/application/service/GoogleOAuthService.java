package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.command.OAuthTokenLoginCommand;
import com.pabom.backend.auth.application.port.GoogleOAuthClientPort;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.domain.model.OAuthState;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService implements OAuthService {

    private final GoogleOAuthClientPort googleOAuthClient;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthAuthorizationResult issueAuthorizationUrl(String appRedirectUri) {
        OAuthState state = appRedirectUri == null ? OAuthState.web() : OAuthState.app(appRedirectUri);
        String authorizationUrl = googleOAuthClient.createAuthorizationUrl(state.value(), state.clientType());
        return new OAuthAuthorizationResult(authorizationUrl, state.value());
    }

    @Override
    public OAuthLoginResult login(OAuthCallbackCommand command) {
        validateState(command.expectedState(), command.actualState());
        if (!StringUtils.hasText(command.authorizationCode())) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }

        SocialUserInfo userInfo = googleOAuthClient.authenticate(
                command.authorizationCode(),
                OAuthState.parse(command.actualState()).clientType()
        );
        return new OAuthLoginResult(
                userInfo.provider(),
                userInfo.providerId(),
                userInfo.nickname()
        );
    }

    @Override
    public OAuthLoginResult loginWithToken(OAuthTokenLoginCommand command) {
        if (!StringUtils.hasText(command.idToken())) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }

        SocialUserInfo userInfo = googleOAuthClient.authenticateWithIdToken(command.idToken());
        return new OAuthLoginResult(
                userInfo.provider(),
                userInfo.providerId(),
                userInfo.nickname()
        );
    }

    private void validateState(String expectedState, String actualState) {
        if (!StringUtils.hasText(expectedState) || !expectedState.equals(actualState)) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }
    }
}
