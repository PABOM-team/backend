package com.pabom.backend.careguideline.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.careguideline.application.result.TodayEmptyReason;
import com.pabom.backend.careguideline.application.result.TodayResult;
import com.pabom.backend.careguideline.application.service.RoutineCommandService;
import com.pabom.backend.careguideline.application.service.TodayQueryService;
import com.pabom.backend.careguideline.application.service.RoutineActionService;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.record.domain.error.RoutineErrorCode;
import java.time.LocalDate;
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

class RoutineControllerTest {
    private TodayQueryService todayService;
    private RoutineCommandService commandService;
    private RoutineActionService actionService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        todayService = org.mockito.Mockito.mock(TodayQueryService.class);
        commandService = org.mockito.Mockito.mock(RoutineCommandService.class);
        actionService = org.mockito.Mockito.mock(RoutineActionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RoutineController(todayService, commandService, actionService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(7L), null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesAuthenticatedRoutineEndpoints() throws Exception {
        given(todayService.get(eq(7L), eq(10L), eq(LocalDate.of(2026, 11, 2))))
                .willReturn(new TodayResult(LocalDate.of(2026, 11, 2),
                        new TodayResult.Summary(0, 0, 0, 0, 0, 0),
                        TodayEmptyReason.NOTHING_DUE, null, List.of()));

        mvc.perform(get("/api/v1/today?petId=10&date=2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.emptyReason").value("NOTHING_DUE"));
        mvc.perform(patch("/api/v1/routines/10").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intervalDays\":5}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/routines/10/reset"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/routines/10/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"c67c4e3d-fd4e-49bd-83f5-3c7d7b64c981\",\"outcome\":\"ATE_ALL\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/routines/10/skip").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"4d0a0e9e-0c2b-4b47-b755-479ecf2ac18b\",\"reasonCode\":\"SCHEDULE\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/routines/10/postpone").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postponeTo\":\"2026-12-10\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void validatesPatchAndDateFormats() throws Exception {
        mvc.perform(patch("/api/v1/routines/10").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intervalDays\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mvc.perform(patch("/api/v1/routines/10").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/today?date=not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesOtherUsersRoutineAndRequiresAuthentication() throws Exception {
        given(commandService.reset(7L, 99L)).willThrow(new BusinessException(RoutineErrorCode.ROUTINE_NOT_FOUND));
        mvc.perform(post("/api/v1/routines/99/reset"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROUTINE_NOT_FOUND"));

        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }
}
