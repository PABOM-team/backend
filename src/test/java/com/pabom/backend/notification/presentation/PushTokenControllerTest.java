package com.pabom.backend.notification.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import com.pabom.backend.notification.application.command.RegisterPushTokenCommand;
import com.pabom.backend.notification.application.service.PushTokenService;
import com.pabom.backend.notification.domain.error.NotificationErrorCode;
import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PushTokenControllerTest {

    private static final String TOKEN = "ExponentPushToken[abc123]";

    private PushTokenService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PushTokenService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PushTokenController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(7L), null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersTokenWithNoContent() throws Exception {
        mvc.perform(put("/api/v1/push-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\",\"platform\":\"IOS\",\"appVariant\":\"DEVELOPMENT\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).register(7L,
                new RegisterPushTokenCommand(TOKEN, PushPlatform.IOS, AppVariant.DEVELOPMENT));
    }

    @Test
    void rejectsMissingOrUnknownEnumValues() throws Exception {
        for (String body : List.of(
                "{\"token\":\"" + TOKEN + "\",\"appVariant\":\"PRODUCTION\"}",
                "{\"token\":\"" + TOKEN + "\",\"platform\":\"WEB\",\"appVariant\":\"PRODUCTION\"}",
                "{\"token\":\"\",\"platform\":\"IOS\",\"appVariant\":\"PRODUCTION\"}")) {
            mvc.perform(put("/api/v1/push-tokens")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void returnsInvalidPushTokenFromService() throws Exception {
        willThrow(new BusinessException(NotificationErrorCode.INVALID_PUSH_TOKEN))
                .given(service).register(eq(7L), any());

        mvc.perform(put("/api/v1/push-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fcm-raw-token\",\"platform\":\"ANDROID\",\"appVariant\":\"PRODUCTION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultType").value("FAIL"))
                .andExpect(jsonPath("$.error.code").value("INVALID_PUSH_TOKEN"));
    }

    @Test
    void unregistersUrlEncodedTokenWithNoContent() throws Exception {
        mvc.perform(delete("/api/v1/push-tokens/{token}", TOKEN))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).unregister(7L, TOKEN);
    }

    @Test
    void requiresAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(delete("/api/v1/push-tokens/{token}", TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
        mvc.perform(put("/api/v1/push-tokens")
                        .requestAttr(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_EXPIRED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\",\"platform\":\"IOS\",\"appVariant\":\"PRODUCTION\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
        verifyNoInteractions(service);
    }
}
