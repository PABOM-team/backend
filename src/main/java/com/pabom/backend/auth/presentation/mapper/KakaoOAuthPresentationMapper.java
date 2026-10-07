package com.pabom.backend.auth.presentation.mapper;

import com.pabom.backend.auth.application.command.KakaoCallbackCommand;
import com.pabom.backend.auth.application.result.KakaoAuthorizationResult;
import com.pabom.backend.auth.application.result.KakaoLoginResult;
import com.pabom.backend.auth.presentation.response.KakaoAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.KakaoLoginResponse;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthPresentationMapper {

    public KakaoCallbackCommand toCommand(
            String authorizationCode,
            String expectedState,
            String actualState
    ) {
        return new KakaoCallbackCommand(authorizationCode, expectedState, actualState);
    }

    public KakaoAuthorizationUrlResponse toResponse(KakaoAuthorizationResult result) {
        return new KakaoAuthorizationUrlResponse(result.authorizationUrl());
    }

    public KakaoLoginResponse toResponse(KakaoLoginResult result) {
        return new KakaoLoginResponse(
                result.provider().name(),
                result.providerId(),
                result.nickname()
        );
    }
}
