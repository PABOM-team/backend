package com.pabom.backend.notification.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.notification.application.command.UpdateNotificationSettingCommand;
import com.pabom.backend.notification.application.result.NotificationSettingResult;
import com.pabom.backend.notification.application.service.NotificationSettingService;
import java.time.LocalTime;
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

class NotificationSettingControllerTest {

    private NotificationSettingService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(NotificationSettingService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NotificationSettingController(service))
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
    void getsSettingsInCommonEnvelopeWithHourMinuteTime() throws Exception {
        given(service.get(7L)).willReturn(
                new NotificationSettingResult(true, LocalTime.of(9, 0), "Asia/Seoul"));

        mvc.perform(get("/api/v1/users/me/notification-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.success.data.careReminderEnabled").value(true))
                .andExpect(jsonPath("$.success.data.careReminderTime").value("09:00"))
                .andExpect(jsonPath("$.success.data.timeZone").value("Asia/Seoul"));
    }

    @Test
    void patchesOnlyGivenFieldsAndReturnsWholeSettings() throws Exception {
        given(service.update(7L, new UpdateNotificationSettingCommand(null, LocalTime.of(21, 30), null)))
                .willReturn(new NotificationSettingResult(true, LocalTime.of(21, 30), "Asia/Seoul"));

        mvc.perform(patch("/api/v1/users/me/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"careReminderTime\":\"21:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.careReminderEnabled").value(true))
                .andExpect(jsonPath("$.success.data.careReminderTime").value("21:30"))
                .andExpect(jsonPath("$.success.data.timeZone").value("Asia/Seoul"));
    }

    @Test
    void rejectsReminderTimeOutsideThirtyMinuteSlots() throws Exception {
        for (String time : List.of("09:15", "9:00", "24:00", "09:00:00", "0900")) {
            mvc.perform(patch("/api/v1/users/me/notification-settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"careReminderTime\":\"" + time + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.error.details.field").value("careReminderTime"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void requiresAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/users/me/notification-settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
        verifyNoInteractions(service);
    }
}
