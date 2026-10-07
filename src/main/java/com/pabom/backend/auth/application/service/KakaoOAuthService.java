package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.command.KakaoCallbackCommand;
import com.pabom.backend.auth.application.port.KakaoOAuthClientPort;
import com.pabom.backend.auth.application.result.KakaoAuthorizationResult;
import com.pabom.backend.auth.application.result.KakaoLoginResult;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.global.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final KakaoOAuthClientPort kakaoOAuthClient;

    public KakaoAuthorizationResult issueAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        String authorizationUrl = kakaoOAuthClient.createAuthorizationUrl(state);
        return new KakaoAuthorizationResult(authorizationUrl, state);
    }

    public KakaoLoginResult login(KakaoCallbackCommand command) {
        validateState(command.expectedState(), command.actualState());
        if (!StringUtils.hasText(command.authorizationCode())) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        SocialUserInfo userInfo = kakaoOAuthClient.authenticate(command.authorizationCode());
        return new KakaoLoginResult(
                userInfo.provider(),
                userInfo.providerId(),
                userInfo.nickname()
        );
    }

    private void validateState(String expectedState, String actualState) {
        if (!StringUtils.hasText(expectedState) || !expectedState.equals(actualState)) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}
