package com.pabom.backend.auth.presentation;

import com.pabom.backend.auth.application.command.GoogleCallbackCommand;
import com.pabom.backend.auth.application.result.GoogleAuthorizationResult;
import com.pabom.backend.auth.application.result.GoogleLoginResult;
import com.pabom.backend.auth.application.service.GoogleOAuthService;
import com.pabom.backend.auth.presentation.docs.GoogleOAuthControllerDocs;
import com.pabom.backend.auth.presentation.mapper.GoogleOAuthPresentationMapper;
import com.pabom.backend.auth.presentation.response.GoogleAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.GoogleLoginResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/v1/auth/google")
public class GoogleOAuthController implements GoogleOAuthControllerDocs {

    private static final String STATE_COOKIE = "google_oauth_state";
    private static final String CALLBACK_PATH = "/api/v1/auth/google/callback";
    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(5);

    private final GoogleOAuthService googleOAuthService;
    private final GoogleOAuthPresentationMapper mapper;
    private final boolean stateCookieSecure;

    public GoogleOAuthController(
            GoogleOAuthService googleOAuthService,
            GoogleOAuthPresentationMapper mapper,
            @Value("${app.google.state-cookie-secure:false}") boolean stateCookieSecure
    ) {
        this.googleOAuthService = googleOAuthService;
        this.mapper = mapper;
        this.stateCookieSecure = stateCookieSecure;
    }

    @Override
    @GetMapping("/login-url")
    public ResponseEntity<ApiResponseBody<GoogleAuthorizationUrlResponse>> issueAuthorizationUrl(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        GoogleAuthorizationResult result = googleOAuthService.issueAuthorizationUrl();
        addStateCookie(response, result.state(), STATE_COOKIE_MAX_AGE);
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), request));
    }

    @Override
    @GetMapping("/callback")
    public ResponseEntity<ApiResponseBody<GoogleLoginResponse>> callback(
            @RequestParam String code,
            @RequestParam String state,
            @Parameter(hidden = true)
            @CookieValue(name = STATE_COOKIE, required = false) String expectedState,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        GoogleCallbackCommand command = mapper.toCommand(code, expectedState, state);
        GoogleLoginResult result = googleOAuthService.login(command);
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
