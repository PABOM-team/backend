package com.pabom.backend.agreement.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.agreement.application.result.AgreementCompletionResult;
import com.pabom.backend.agreement.application.service.AgreementCompletionService;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.user.domain.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AgreementControllerTest {

    private AgreementCompletionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AgreementCompletionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AgreementController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsActivatedUserAndAccessToken() throws Exception {
        given(service.complete(eq("signup-token"), any())).willReturn(
                new AgreementCompletionResult(101L, UserStatus.ACTIVE, "access-token", 1800)
        );

        mockMvc.perform(post("/api/v1/me/agreements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(101))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void rejectsUnknownAgreementTypeAsValidationFailure() throws Exception {
        mockMvc.perform(post("/api/v1/me/agreements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("MARKETING", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/me/agreements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingRequiredRequestField() throws Exception {
        mockMvc.perform(post("/api/v1/me/agreements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer signup-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreements\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(service);
    }

    private String validBody() {
        return """
                {
                  "agreements": [
                    {"type":"TERMS","version":"1.0","agreed":true},
                    {"type":"PRIVACY","version":"1.0","agreed":true},
                    {"type":"AGE_14","version":"1.0","agreed":true},
                    {"type":"MARKETING","version":"1.0","agreed":false}
                  ]
                }
                """;
    }
}
