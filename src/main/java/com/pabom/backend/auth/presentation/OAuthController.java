package com.pabom.backend.auth.presentation;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.application.service.OAuthService;
import com.pabom.backend.auth.application.service.OAuthServiceResolver;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.presentation.docs.OAuthControllerDocs;
import com.pabom.backend.auth.presentation.mapper.OAuthPresentationMapper;
import com.pabom.backend.auth.presentation.response.OAuthAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.OAuthLoginResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/{provider}")
public class OAuthController implements OAuthControllerDocs {

    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(5);

    private final OAuthServiceResolver serviceResolver;
    private final OAuthPresentationMapper mapper;
    private final boolean googleStateCookieSecure;
    private final boolean kakaoStateCookieSecure;

    public OAuthController(
            OAuthServiceResolver serviceResolver,
            OAuthPresentationMapper mapper,
            @Value("${app.google.state-cookie-secure:false}") boolean googleStateCookieSecure,
            @Value("${app.kakao.state-cookie-secure:false}") boolean kakaoStateCookieSecure
    ) {
        this.serviceResolver = serviceResolver;
        this.mapper = mapper;
        this.googleStateCookieSecure = googleStateCookieSecure;
        this.kakaoStateCookieSecure = kakaoStateCookieSecure;
    }

    @Override
    @GetMapping("/login-url")
    public ResponseEntity<ApiResponseBody<OAuthAuthorizationUrlResponse>> issueAuthorizationUrl(
            @PathVariable String provider,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        OAuthService service = serviceResolver.resolve(provider);
        OAuthProvider oauthProvider = service.provider();
        OAuthAuthorizationResult result = service.issueAuthorizationUrl();
        addStateCookie(response, oauthProvider, result.state(), STATE_COOKIE_MAX_AGE);
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), request));
    }

    @Override
    @GetMapping("/callback")
    public ResponseEntity<ApiResponseBody<OAuthLoginResponse>> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        OAuthService service = serviceResolver.resolve(provider);
        OAuthProvider oauthProvider = service.provider();
        OAuthCallbackCommand command = mapper.toCommand(code, findStateCookie(request, oauthProvider), state);
        OAuthLoginResult result = service.login(command);
        addStateCookie(response, oauthProvider, "", Duration.ZERO);
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), request));
    }

    private String findStateCookie(HttpServletRequest request, OAuthProvider provider) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName(provider).equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void addStateCookie(
            HttpServletResponse response,
            OAuthProvider provider,
            String value,
            Duration maxAge
    ) {
        ResponseCookie cookie = ResponseCookie.from(cookieName(provider), value)
                .httpOnly(true)
                .secure(isStateCookieSecure(provider))
                .sameSite("Lax")
                .path(callbackPath(provider))
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String cookieName(OAuthProvider provider) {
        return provider.name().toLowerCase(Locale.ROOT) + "_oauth_state";
    }

    private String callbackPath(OAuthProvider provider) {
        return "/api/v1/auth/" + provider.name().toLowerCase(Locale.ROOT) + "/callback";
    }

    private boolean isStateCookieSecure(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleStateCookieSecure;
            case KAKAO -> kakaoStateCookieSecure;
        };
    }
}
