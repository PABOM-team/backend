package com.pabom.backend.notification.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.notification.application.result.PushDeliveryResult;
import com.pabom.backend.notification.application.service.PushTestService;
import com.pabom.backend.notification.domain.error.NotificationErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PushTestControllerTest {

    private PushTestService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PushTestService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PushTestController(service))
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
    void isRegisteredOnlyForLocalAndDevProfiles() {
        org.assertj.core.api.Assertions.assertThat(
                        PushTestController.class.getAnnotation(Profile.class).value())
                .containsExactlyInAnyOrder("local", "dev");
    }

    @Test
    void sendsToMyDevices() throws Exception {
        given(service.sendToMyDevices(7L)).willReturn(new PushDeliveryResult(2, 1));

        mvc.perform(post("/api/v1/push-tokens/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.targetCount").value(2))
                .andExpect(jsonPath("$.success.data.acceptedCount").value(1));
    }

    @Test
    void mapsMissingTokenAndMissingConfiguration() throws Exception {
        given(service.sendToMyDevices(7L))
                .willThrow(new BusinessException(NotificationErrorCode.PUSH_TOKEN_NOT_FOUND))
                .willThrow(new BusinessException(NotificationErrorCode.PUSH_NOT_CONFIGURED));

        mvc.perform(post("/api/v1/push-tokens/test"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PUSH_TOKEN_NOT_FOUND"));
        mvc.perform(post("/api/v1/push-tokens/test"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("PUSH_NOT_CONFIGURED"));
    }
}
