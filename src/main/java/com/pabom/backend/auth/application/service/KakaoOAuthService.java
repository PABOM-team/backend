package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.port.KakaoOAuthClientPort;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.global.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService implements OAuthService {

    private final KakaoOAuthClientPort kakaoOAuthClient;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthAuthorizationResult issueAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        String authorizationUrl = kakaoOAuthClient.createAuthorizationUrl(state);
        return new OAuthAuthorizationResult(authorizationUrl, state);
    }

    @Override
    public OAuthLoginResult login(OAuthCallbackCommand command) {
        validateState(command.expectedState(), command.actualState());
        if (!StringUtils.hasText(command.authorizationCode())) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }

        SocialUserInfo userInfo = kakaoOAuthClient.authenticate(command.authorizationCode());
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
