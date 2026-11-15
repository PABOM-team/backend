package com.pabom.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.command.OAuthTokenLoginCommand;
import com.pabom.backend.auth.application.port.KakaoOAuthClientPort;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.model.OAuthClientType;
import com.pabom.backend.auth.domain.model.OAuthState;
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
        given(kakaoOAuthClient.createAuthorizationUrl(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(OAuthClientType.WEB)
        ))
                .willAnswer(invocation -> "https://kauth.kakao.com/oauth/authorize?state="
                        + invocation.getArgument(0, String.class));

        OAuthAuthorizationResult result = kakaoOAuthService.issueAuthorizationUrl(null);

        assertThat(result.state()).isNotBlank().doesNotContain(".");
        assertThat(result.authorizationUrl()).endsWith(result.state());
    }

    @Test
    void issuesAppAuthorizationUrlWithAppRedirectInState() {
        given(kakaoOAuthClient.createAuthorizationUrl(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(OAuthClientType.APP)
        )).willAnswer(invocation -> "https://provider.example/authorize?state="
                + invocation.getArgument(0, String.class));

        OAuthAuthorizationResult result = kakaoOAuthService.issueAuthorizationUrl("pabom://auth/callback/kakao");

        OAuthState state = OAuthState.parse(result.state());
        assertThat(state.clientType()).isEqualTo(OAuthClientType.APP);
        assertThat(state.appRedirectUri()).isEqualTo("pabom://auth/callback/kakao");
        assertThat(result.authorizationUrl()).endsWith(result.state());
    }

    @Test
    void exchangesCodeWithAppRedirectUriWhenStateIsAppState() {
        String appState = OAuthState.app("pabom-dev://auth/callback/kakao").value();
        given(kakaoOAuthClient.authenticate("authorization-code", OAuthClientType.APP))
                .willReturn(SocialUserInfo.kakao("123456", "파봄"));

        OAuthLoginResult result = kakaoOAuthService.login(
                new OAuthCallbackCommand("authorization-code", appState, appState)
        );

        assertThat(result.providerId()).isEqualTo("123456");
    }

    @Test
    void returnsKakaoUserInfoWhenStateMatches() {
        given(kakaoOAuthClient.authenticate("authorization-code", OAuthClientType.WEB))
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
                        .getCode()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void returnsKakaoUserInfoForSdkToken() {
        given(kakaoOAuthClient.authenticateWithAccessToken("sdk-access-token"))
                .willReturn(SocialUserInfo.kakao("123456", "파봄"));

        OAuthLoginResult result = kakaoOAuthService.loginWithToken(new OAuthTokenLoginCommand("sdk-access-token", null));

        assertThat(result.provider().name()).isEqualTo("KAKAO");
        assertThat(result.providerId()).isEqualTo("123456");
        assertThat(result.nickname()).isEqualTo("파봄");
    }

    @Test
    void rejectsTokenLoginWithoutAccessToken() {
        assertThatThrownBy(() -> kakaoOAuthService.loginWithToken(new OAuthTokenLoginCommand(null, "sdk-access-token")))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode()
                        .getCode()).isEqualTo("VALIDATION_FAILED"));
        verifyNoInteractions(kakaoOAuthClient);
    }
}
