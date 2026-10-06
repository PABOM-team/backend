package com.pabom.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pabom.backend.auth.application.command.GoogleCallbackCommand;
import com.pabom.backend.auth.application.port.GoogleOAuthClientPort;
import com.pabom.backend.auth.application.result.GoogleAuthorizationResult;
import com.pabom.backend.auth.application.result.GoogleLoginResult;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.global.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleOAuthServiceTest {

    private final GoogleOAuthClientPort googleOAuthClient = mock(GoogleOAuthClientPort.class);
    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        googleOAuthService = new GoogleOAuthService(googleOAuthClient);
    }

    @Test
    void issuesAuthorizationUrlWithGeneratedState() {
        given(googleOAuthClient.createAuthorizationUrl(org.mockito.ArgumentMatchers.anyString()))
                .willAnswer(invocation -> "https://accounts.google.com/o/oauth2/v2/auth?state="
                        + invocation.getArgument(0, String.class));

        GoogleAuthorizationResult result = googleOAuthService.issueAuthorizationUrl();

        assertThat(result.state()).isNotBlank();
        assertThat(result.authorizationUrl()).endsWith(result.state());
    }

    @Test
    void returnsGoogleUserInfoWhenStateMatches() {
        given(googleOAuthClient.authenticate("authorization-code"))
                .willReturn(SocialUserInfo.google("123456", "파봄", "pabom@example.com"));

        GoogleLoginResult result = googleOAuthService.login(
                new GoogleCallbackCommand("authorization-code", "state", "state")
        );

        assertThat(result.provider().name()).isEqualTo("GOOGLE");
        assertThat(result.providerId()).isEqualTo("123456");
        assertThat(result.nickname()).isEqualTo("파봄");
        assertThat(result.email()).isEqualTo("pabom@example.com");
    }

    @Test
    void rejectsCallbackWhenStateDoesNotMatch() {
        GoogleCallbackCommand command = new GoogleCallbackCommand(
                "authorization-code",
                "expected-state",
                "tampered-state"
        );

        assertThatThrownBy(() -> googleOAuthService.login(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode()
                        .getCode()).isEqualTo("INVALID_SOCIAL_TOKEN"));
    }
}
