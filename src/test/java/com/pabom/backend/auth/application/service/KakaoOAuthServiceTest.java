package com.pabom.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.port.KakaoOAuthClientPort;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.global.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KakaoOAuthServiceTest {

    private final KakaoOAuthClientPort kakaoOAuthClient = mock(KakaoOAuthClientPort.class);
    private KakaoOAuthService kakaoOAuthService;

    @BeforeEach
    void setUp() {
        kakaoOAuthService = new KakaoOAuthService(kakaoOAuthClient);
    }

    @Test
    void issuesAuthorizationUrlWithGeneratedState() {
        given(kakaoOAuthClient.createAuthorizationUrl(org.mockito.ArgumentMatchers.anyString()))
                .willAnswer(invocation -> "https://kauth.kakao.com/oauth/authorize?state="
                        + invocation.getArgument(0, String.class));

        OAuthAuthorizationResult result = kakaoOAuthService.issueAuthorizationUrl();

        assertThat(result.state()).isNotBlank();
        assertThat(result.authorizationUrl()).endsWith(result.state());
    }

    @Test
    void returnsKakaoUserInfoWhenStateMatches() {
        given(kakaoOAuthClient.authenticate("authorization-code"))
                .willReturn(SocialUserInfo.kakao("123456", "파봄"));

        OAuthLoginResult result = kakaoOAuthService.login(
                new OAuthCallbackCommand("authorization-code", "state", "state")
        );

        assertThat(result.provider().name()).isEqualTo("KAKAO");
        assertThat(result.providerId()).isEqualTo("123456");
        assertThat(result.nickname()).isEqualTo("파봄");
    }

    @Test
    void rejectsCallbackWhenStateDoesNotMatch() {
        OAuthCallbackCommand command = new OAuthCallbackCommand(
                "authorization-code",
                "expected-state",
                "tampered-state"
        );

        assertThatThrownBy(() -> kakaoOAuthService.login(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode()
                        .getCode()).isEqualTo("INVALID_SOCIAL_TOKEN"));
    }
}
