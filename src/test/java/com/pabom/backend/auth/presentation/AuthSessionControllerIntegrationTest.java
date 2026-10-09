package com.pabom.backend.auth.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.auth.application.result.TokenRefreshResult;
import com.pabom.backend.auth.application.service.AuthSessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthSessionService authSessionService;

    @Test
    void refreshesTokensAndRotatesCookie() throws Exception {
        given(authSessionService.refresh("refresh-token")).willReturn(
                new TokenRefreshResult("access-token", 1800, "next-refresh-token", 1209600)
        );

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .cookie(new Cookie("pabom_rt", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("pabom_rt=next-refresh-token"),
                                org.hamcrest.Matchers.containsString("Max-Age=1209600"),
                                org.hamcrest.Matchers.containsString("Path=/api/v1/auth"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax")
                        )))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void rejectsDisallowedOriginWithContractError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .cookie(new Cookie("pabom_rt", "refresh-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORIGIN_NOT_ALLOWED"));
    }

    @Test
    void logoutAlwaysReturnsNoContentAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8080")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer malformed-token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("pabom_rt="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("Path=/api/v1/auth")
                        )));

        verify(authSessionService).logout(null);
    }
}
