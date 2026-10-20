package com.pabom.backend.record.presentation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pabom.backend.global.error.*;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.record.application.service.RecordService;
import com.pabom.backend.record.domain.error.RecordErrorCode;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecordControllerTest {
    private RecordService service; private MockMvc mvc;
    @BeforeEach void setup() {
        service=org.mockito.Mockito.mock(RecordService.class);
        mvc=MockMvcBuilders.standaloneSetup(new RecordController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AuthUser(7L),null,List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void exposesSixAuthenticatedEndpointsWithSpecifiedStatuses() throws Exception {
        mvc.perform(get("/api/v1/records")).andExpect(status().isOk()).andExpect(jsonPath("$.resultType").value("SUCCESS"));
        mvc.perform(post("/api/v1/records").contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/records/1")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/records/1").contentType(MediaType.APPLICATION_JSON).content("{\"revisionNo\":1,\"memo\":\"수정\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/records/1")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/records/1/restore")).andExpect(status().isOk());
    }

    @Test void validatesBodiesAndQueryValues() throws Exception {
        mvc.perform(post("/api/v1/records").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mvc.perform(patch("/api/v1/records/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        given(service.list(eq(7L),isNull(),isNull(),isNull(),isNull(),isNull(),eq(false),isNull(),eq(51)))
                .willThrow(new BusinessException(com.pabom.backend.auth.domain.error.AuthErrorCode.VALIDATION_FAILED));
        mvc.perform(get("/api/v1/records?size=51")).andExpect(status().isBadRequest());
    }

    @Test void hidesOtherUsersRecordsAs404() throws Exception {
        given(service.get(7L,99L)).willThrow(new BusinessException(RecordErrorCode.RECORD_NOT_FOUND));
        mvc.perform(get("/api/v1/records/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECORD_NOT_FOUND"));
    }

    @Test void requiresBearerAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/records")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    private String createBody() { return """
        {"clientRequestId":"a1b2c3d4-e5f6-47a8-9012-123456789abc","petId":12,"itemCode":"FEEDING","occurredAt":"2026-10-20T10:00:00+09:00"}
        """; }
}
