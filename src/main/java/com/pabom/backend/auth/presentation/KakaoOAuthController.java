package com.pabom.backend.auth.presentation;

import com.pabom.backend.auth.application.command.KakaoCallbackCommand;
import com.pabom.backend.auth.application.result.KakaoAuthorizationResult;
import com.pabom.backend.auth.application.result.KakaoLoginResult;
import com.pabom.backend.auth.application.service.KakaoOAuthService;
import com.pabom.backend.auth.presentation.docs.KakaoOAuthControllerDocs;
import com.pabom.backend.auth.presentation.mapper.KakaoOAuthPresentationMapper;
import com.pabom.backend.auth.presentation.response.KakaoAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.KakaoLoginResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/kakao")
public class KakaoOAuthController implements KakaoOAuthControllerDocs {

    private static final String STATE_COOKIE = "kakao_oauth_state";
    private static final String CALLBACK_PATH = "/api/v1/auth/kakao/callback";
    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(5);

    private final KakaoOAuthService kakaoOAuthService;
    private final KakaoOAuthPresentationMapper mapper;
    private final boolean stateCookieSecure;

    public KakaoOAuthController(
            KakaoOAuthService kakaoOAuthService,
            KakaoOAuthPresentationMapper mapper,
            @Value("${app.kakao.state-cookie-secure:false}") boolean stateCookieSecure
    ) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.mapper = mapper;
        this.stateCookieSecure = stateCookieSecure;
    }

    @Override
    @GetMapping("/login-url")
    public ResponseEntity<ApiResponseBody<KakaoAuthorizationUrlResponse>> issueAuthorizationUrl(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        KakaoAuthorizationResult result = kakaoOAuthService.issueAuthorizationUrl();
        addStateCookie(response, result.state(), STATE_COOKIE_MAX_AGE);
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), request));
    }

    @Override
    @GetMapping("/callback")
    public ResponseEntity<ApiResponseBody<KakaoLoginResponse>> callback(
            @RequestParam String code,
            @RequestParam String state,
            @Parameter(hidden = true)
            @CookieValue(name = STATE_COOKIE, required = false) String expectedState,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        KakaoCallbackCommand command = mapper.toCommand(code, expectedState, state);
        KakaoLoginResult result = kakaoOAuthService.login(command);
        addStateCookie(response, "", Duration.ZERO);
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), request));
    }

    private void addStateCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .secure(stateCookieSecure)
                .sameSite("Lax")
                .path(CALLBACK_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
