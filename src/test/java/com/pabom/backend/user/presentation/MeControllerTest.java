package com.pabom.backend.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.infrastructure.config.AuthWebProperties;
import com.pabom.backend.auth.presentation.cookie.RefreshTokenCookieManager;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import com.pabom.backend.user.application.result.UserProfileResult;
import com.pabom.backend.user.application.result.UserProfileResult.AgreementItem;
import com.pabom.backend.user.application.result.UserProfileResult.PetCount;
import com.pabom.backend.user.application.result.UserProfileResult.RequiredAgreementItem;
import com.pabom.backend.user.application.service.UserProfileService;
import com.pabom.backend.user.application.service.WithdrawalService;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerTest {

    private UserProfileService profileService;
    private WithdrawalService withdrawalService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        profileService = org.mockito.Mockito.mock(UserProfileService.class);
        withdrawalService = org.mockito.Mockito.mock(WithdrawalService.class);
        RefreshTokenCookieManager cookies = new RefreshTokenCookieManager(new AuthWebProperties(
                List.of("http://localhost"), false, "Lax"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserController(profileService, withdrawalService, cookies))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate();
    }

    @Test
    void getsPendingUserProfileWithRoleAgreementsAndPetCounts() throws Exception {
        given(profileService.get(101L)).willReturn(new UserProfileResult(
                101L, "테스트", UserStatus.PENDING_TERMS, UserRole.ADMIN, null,
                List.of(new AgreementItem(AgreementType.TERMS, "1.0", true,
                        Instant.parse("2026-10-17T03:00:00Z"))),
                List.of(new RequiredAgreementItem(AgreementType.TERMS, "1.0", true)),
                false, new PetCount(2, 1), Instant.parse("2026-10-17T02:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.id").value(101))
                .andExpect(jsonPath("$.success.data.status").value("PENDING_TERMS"))
                .andExpect(jsonPath("$.success.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.success.data.region").isEmpty())
                .andExpect(jsonPath("$.success.data.petCount.active").value(2))
                .andExpect(jsonPath("$.success.data.petCount.ended").value(1));
    }

    @Test
    void distinguishesMissingExpiredAndInvalidAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
        mockMvc.perform(get("/api/v1/users/me")
                        .requestAttr(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE,
                                AuthErrorCode.TOKEN_EXPIRED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
        mockMvc.perform(get("/api/v1/users/me")
                        .requestAttr(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE,
                                AuthErrorCode.TOKEN_INVALID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    void rejectsMissingNullOrFalseConfirmationWith422() throws Exception {
        for (String body : List.of("{}", "{\"confirm\":null}", "{\"confirm\":false}")) {
            mockMvc.perform(delete("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("CONFIRM_REQUIRED"));
        }
    }

    @Test
    void withdrawsAndClearsRefreshCookieWithNoBody() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":true}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("pabom_rt="),
                                org.hamcrest.Matchers.containsString("Path=/api/v1/auth"),
                                org.hamcrest.Matchers.containsString("Max-Age=0"))));
        verify(withdrawalService).withdraw(101L);
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(101L), null, List.of()));
    }
}
