package com.pabom.backend.auth.presentation;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginCompletionResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.application.result.TokenRefreshResult;
import com.pabom.backend.auth.application.service.AuthSessionService;
import com.pabom.backend.auth.application.service.OAuthLoginCompletionService;
import com.pabom.backend.auth.application.service.OAuthService;
import com.pabom.backend.auth.application.service.OAuthServiceResolver;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthClientType;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.domain.model.OAuthState;
import com.pabom.backend.auth.presentation.cookie.RefreshTokenCookieManager;
import com.pabom.backend.auth.presentation.docs.AuthControllerDocs;
import com.pabom.backend.auth.presentation.mapper.OAuthPresentationMapper;
import com.pabom.backend.auth.presentation.redirect.AppRedirectUriPolicy;
import com.pabom.backend.auth.presentation.request.SocialTokenLoginRequest;
import com.pabom.backend.auth.presentation.response.OAuthAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.OAuthLoginResponse;
import com.pabom.backend.auth.presentation.response.TokenRefreshResponse;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocs {

    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(5);

    private final OAuthServiceResolver serviceResolver;
    private final OAuthLoginCompletionService loginCompletionService;
    private final AuthSessionService authSessionService;
    private final OAuthPresentationMapper mapper;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final AppRedirectUriPolicy appRedirectUriPolicy;
    private final boolean googleStateCookieSecure;
    private final boolean kakaoStateCookieSecure;

    public AuthController(
            OAuthServiceResolver serviceResolver,
            OAuthLoginCompletionService loginCompletionService,
            AuthSessionService authSessionService,
            OAuthPresentationMapper mapper,
            RefreshTokenCookieManager refreshTokenCookieManager,
            AppRedirectUriPolicy appRedirectUriPolicy,
            @Value("${app.google.state-cookie-secure:false}") boolean googleStateCookieSecure,
            @Value("${app.kakao.state-cookie-secure:false}") boolean kakaoStateCookieSecure
    ) {
        this.serviceResolver = serviceResolver;
        this.loginCompletionService = loginCompletionService;
        this.authSessionService = authSessionService;
        this.mapper = mapper;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
        this.appRedirectUriPolicy = appRedirectUriPolicy;
        this.googleStateCookieSecure = googleStateCookieSecure;
        this.kakaoStateCookieSecure = kakaoStateCookieSecure;
    }

    @Override
    @GetMapping("/{provider}/login-url")
    public ResponseEntity<ApiResponseBody<OAuthAuthorizationUrlResponse>> issueAuthorizationUrl(
            @PathVariable String provider,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        OAuthService service = serviceResolver.resolve(provider);
        OAuthProvider oauthProvider = service.provider();
        if (redirectUri != null) {
            appRedirectUriPolicy.validate(oauthProvider, redirectUri);
        }
        OAuthAuthorizationResult result = service.issueAuthorizationUrl(redirectUri);
        addStateCookie(response, oauthProvider, result.state(), STATE_COOKIE_MAX_AGE);
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), request));
    }

    @Override
    @GetMapping("/{provider}/callback")
    public ResponseEntity<OAuthLoginResponse> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        OAuthService service = serviceResolver.resolve(provider);
        OAuthProvider oauthProvider = service.provider();
        OAuthCallbackCommand command = mapper.toCommand(code, findStateCookie(request, oauthProvider), state);
        OAuthLoginResult socialLogin = service.login(command);
        OAuthLoginCompletionResult result = loginCompletionService.complete(socialLogin);
        addStateCookie(response, oauthProvider, "", Duration.ZERO);
        if (result.refreshToken() != null) {
            refreshTokenCookieManager.add(
                    response,
                    result.refreshToken(),
                    result.refreshExpiresIn()
            );
        }
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @Override
    @GetMapping("/{provider}/app-callback")
    public ResponseEntity<Void> appCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        OAuthProvider oauthProvider = serviceResolver.resolve(provider).provider();
        OAuthState oauthState = OAuthState.parse(state);
        if (oauthState.clientType() != OAuthClientType.APP) {
            throw new BusinessException(AuthErrorCode.REDIRECT_URI_NOT_ALLOWED);
        }
        appRedirectUriPolicy.validate(oauthProvider, oauthState.appRedirectUri());

        UriComponentsBuilder location = UriComponentsBuilder.fromUriString(oauthState.appRedirectUri());
        if (StringUtils.hasText(error)) {
            addEncodedQueryParam(location, "error", error);
            if (StringUtils.hasText(errorDescription)) {
                addEncodedQueryParam(location, "message", errorDescription);
            }
        } else if (!StringUtils.hasText(code)) {
            addEncodedQueryParam(location, "error", AuthErrorCode.VALIDATION_FAILED.getCode());
            addEncodedQueryParam(location, "message", AuthErrorCode.VALIDATION_FAILED.getMessage());
        } else {
            addEncodedQueryParam(location, "code", code);
        }
        addEncodedQueryParam(location, "state", state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.build(true).toUriString())
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @Override
    @PostMapping("/{provider}/token")
    public ResponseEntity<ApiResponseBody<OAuthLoginResponse>> loginWithToken(
            @PathVariable String provider,
            @RequestBody SocialTokenLoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        OAuthService service = serviceResolver.resolve(provider);
        OAuthLoginResult socialLogin = service.loginWithToken(mapper.toCommand(request));
        OAuthLoginCompletionResult result = loginCompletionService.complete(socialLogin);
        if (result.refreshToken() != null) {
            refreshTokenCookieManager.add(
                    response,
                    result.refreshToken(),
                    result.refreshExpiresIn()
            );
        }
        return ResponseEntity.ok(ApiResponseBody.success(mapper.toResponse(result), httpRequest));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue(name = RefreshTokenCookieManager.COOKIE_NAME, required = false)
            String refreshToken,
            HttpServletResponse response
    ) {
        TokenRefreshResult result = authSessionService.refresh(refreshToken);
        refreshTokenCookieManager.add(
                response,
                result.refreshToken(),
                result.refreshExpiresIn()
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TokenRefreshResponse(result.accessToken(), result.expiresIn()));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieManager.COOKIE_NAME, required = false)
            String refreshToken,
            HttpServletResponse response
    ) {
        try {
            authSessionService.logout(refreshToken);
        } finally {
            refreshTokenCookieManager.clear(response);
        }
        return ResponseEntity.noContent().build();
    }

    private void addEncodedQueryParam(UriComponentsBuilder builder, String name, String value) {
        builder.queryParam(name, UriUtils.encode(value, StandardCharsets.UTF_8));
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
