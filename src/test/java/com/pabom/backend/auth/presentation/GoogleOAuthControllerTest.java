package com.pabom.backend.auth.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.auth.application.command.GoogleCallbackCommand;
import com.pabom.backend.auth.application.result.GoogleAuthorizationResult;
import com.pabom.backend.auth.application.result.GoogleLoginResult;
import com.pabom.backend.auth.application.service.GoogleOAuthService;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.presentation.mapper.GoogleOAuthPresentationMapper;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GoogleOAuthControllerTest {

    private final GoogleOAuthService googleOAuthService = Mockito.mock(GoogleOAuthService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GoogleOAuthController controller = new GoogleOAuthController(
                googleOAuthService,
                new GoogleOAuthPresentationMapper(),
                false
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAuthorizationUrlAndStateCookie() throws Exception {
        given(googleOAuthService.issueAuthorizationUrl()).willReturn(
                new GoogleAuthorizationResult(
                        "https://accounts.google.com/o/oauth2/v2/auth?state=state-value",
                        "state-value"
                )
        );

        mockMvc.perform(get("/api/v1/auth/google/login-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.success.data.authorizationUrl")
                        .value("https://accounts.google.com/o/oauth2/v2/auth?state=state-value"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.meta.path").value("/api/v1/auth/google/login-url"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("google_oauth_state=state-value")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));
    }

    @Test
    void returnsGoogleUserInfoFromCallback() throws Exception {
        given(googleOAuthService.login(ArgumentMatchers.any(GoogleCallbackCommand.class)))
                .willReturn(new GoogleLoginResult(
                        OAuthProvider.GOOGLE,
                        "123456",
                        "파봄"
                ));

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "authorization-code")
                        .param("state", "state-value")
                        .cookie(new Cookie("google_oauth_state", "state-value")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.success.data.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.success.data.providerId").value("123456"))
                .andExpect(jsonPath("$.success.data.nickname").value("파봄"))
                .andExpect(jsonPath("$.success.data.email").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void returnsKakaoErrorFormatForInvalidSocialToken() throws Exception {
        given(googleOAuthService.login(ArgumentMatchers.any(GoogleCallbackCommand.class)))
                .willThrow(new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN));

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "invalid-code")
                        .param("state", "invalid-state")
                        .cookie(new Cookie("google_oauth_state", "expected-state")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultType").value("FAIL"))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("INVALID_SOCIAL_TOKEN"))
                .andExpect(jsonPath("$.error.message").value("유효하지 않은 소셜 토큰입니다."))
                .andExpect(jsonPath("$.error.details").doesNotExist())
                .andExpect(jsonPath("$.meta.path").value("/api/v1/auth/google/callback"));
    }
}
