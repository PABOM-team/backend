package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.command.GoogleCallbackCommand;
import com.pabom.backend.auth.application.port.GoogleOAuthClientPort;
import com.pabom.backend.auth.application.result.GoogleAuthorizationResult;
import com.pabom.backend.auth.application.result.GoogleLoginResult;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.global.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleOAuthClientPort googleOAuthClient;

    public GoogleAuthorizationResult issueAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        String authorizationUrl = googleOAuthClient.createAuthorizationUrl(state);
        return new GoogleAuthorizationResult(authorizationUrl, state);
    }

    public GoogleLoginResult login(GoogleCallbackCommand command) {
        validateState(command.expectedState(), command.actualState());
        if (!StringUtils.hasText(command.authorizationCode())) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        SocialUserInfo userInfo = googleOAuthClient.authenticate(command.authorizationCode());
        return new GoogleLoginResult(
                userInfo.provider(),
                userInfo.providerId(),
                userInfo.nickname(),
                userInfo.email()
        );
    }

    private void validateState(String expectedState, String actualState) {
        if (!StringUtils.hasText(expectedState) || !expectedState.equals(actualState)) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}
