package com.pabom.backend.auth.presentation;

import com.pabom.backend.auth.application.result.TokenRefreshResult;
import com.pabom.backend.auth.application.service.AuthSessionService;
import com.pabom.backend.auth.presentation.cookie.RefreshTokenCookieManager;
import com.pabom.backend.auth.presentation.docs.AuthSessionControllerDocs;
import com.pabom.backend.auth.presentation.response.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthSessionController implements AuthSessionControllerDocs {

    private final AuthSessionService authSessionService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    public AuthSessionController(
            AuthSessionService authSessionService,
            RefreshTokenCookieManager refreshTokenCookieManager
    ) {
        this.authSessionService = authSessionService;
        this.refreshTokenCookieManager = refreshTokenCookieManager;
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
}
