package com.pabom.backend.auth.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.application.service.OAuthService;
import com.pabom.backend.auth.application.service.OAuthServiceResolver;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.presentation.mapper.OAuthPresentationMapper;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OAuthControllerTest {

    private OAuthServiceResolver serviceResolver;
    private OAuthService googleOAuthService;
    private OAuthService kakaoOAuthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        serviceResolver = mock(OAuthServiceResolver.class);
        googleOAuthService = mock(OAuthService.class);
        kakaoOAuthService = mock(OAuthService.class);
        given(serviceResolver.resolve("google")).willReturn(googleOAuthService);
        given(serviceResolver.resolve("kakao")).willReturn(kakaoOAuthService);
        given(serviceResolver.resolve("naver"))
                .willThrow(new BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
        given(googleOAuthService.provider()).willReturn(OAuthProvider.GOOGLE);
        given(kakaoOAuthService.provider()).willReturn(OAuthProvider.KAKAO);

        OAuthController controller = new OAuthController(
                serviceResolver,
                new OAuthPresentationMapper(),
                false,
                false
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void routesGoogleAuthorizationUrlRequestAndSetsGoogleStateCookie() throws Exception {
        given(googleOAuthService.issueAuthorizationUrl()).willReturn(
                new OAuthAuthorizationResult(
                        "https://accounts.google.com/o/oauth2/v2/auth?state=state-value",
                        "state-value"
                )
        );

        mockMvc.perform(get("/api/v1/auth/google/login-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.authorizationUrl")
                        .value("https://accounts.google.com/o/oauth2/v2/auth?state=state-value"))
                .andExpect(jsonPath("$.meta.path").value("/api/v1/auth/google/login-url"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("google_oauth_state=state-value")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("Path=/api/v1/auth/google/callback")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));

        verify(googleOAuthService).issueAuthorizationUrl();
    }

    @Test
    void routesKakaoAuthorizationUrlRequestAndSetsKakaoStateCookie() throws Exception {
        given(kakaoOAuthService.issueAuthorizationUrl()).willReturn(
                new OAuthAuthorizationResult(
                        "https://kauth.kakao.com/oauth/authorize?state=state-value",
                        "state-value"
                )
        );

        mockMvc.perform(get("/api/v1/auth/kakao/login-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.authorizationUrl")
                        .value("https://kauth.kakao.com/oauth/authorize?state=state-value"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("kakao_oauth_state=state-value")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("Path=/api/v1/auth/kakao/callback")));

        verify(kakaoOAuthService).issueAuthorizationUrl();
    }

    @Test
    void usesOnlyGoogleStateCookieForGoogleCallback() throws Exception {
        given(googleOAuthService.login(argThat(command ->
                "google-state".equals(command.expectedState())
        ))).willReturn(new OAuthLoginResult(OAuthProvider.GOOGLE, "123456", "파봄"));

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "authorization-code")
                        .param("state", "google-state")
                        .cookie(
                                new Cookie("kakao_oauth_state", "kakao-state"),
                                new Cookie("google_oauth_state", "google-state")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.success.data.providerId").value("123456"))
                .andExpect(jsonPath("$.success.data.nickname").value("파봄"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("google_oauth_state=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void returnsBadRequestForUnsupportedProvider() throws Exception {
        mockMvc.perform(get("/api/v1/auth/naver/login-url"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_OAUTH_PROVIDER"))
                .andExpect(jsonPath("$.error.message").value("지원하지 않는 OAuth 제공자입니다."));
    }

    @Test
    void returnsUnauthorizedForInvalidSocialToken() throws Exception {
        given(googleOAuthService.login(argThat(command -> true)))
                .willThrow(new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN));

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "invalid-code")
                        .param("state", "invalid-state")
                        .cookie(new Cookie("google_oauth_state", "expected-state")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_SOCIAL_TOKEN"))
                .andExpect(jsonPath("$.meta.path").value("/api/v1/auth/google/callback"));
    }
}
