package com.pabom.backend.notification.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BaseErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import com.pabom.backend.notification.application.command.RegisterPushTokenCommand;
import com.pabom.backend.notification.application.service.PushTokenService;
import com.pabom.backend.notification.presentation.docs.PushTokenControllerDocs;
import com.pabom.backend.notification.presentation.request.RegisterPushTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push-tokens")
@RequiredArgsConstructor
public class PushTokenController implements PushTokenControllerDocs {

    private final PushTokenService pushTokenService;

    @Override
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RegisterPushTokenRequest body,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser, request);
        pushTokenService.register(authUser.userId(),
                new RegisterPushTokenCommand(body.token(), body.platform(), body.appVariant()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String token,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser, request);
        pushTokenService.unregister(authUser.userId(), token);
        return ResponseEntity.noContent().build();
    }

    private void requireAuthentication(AuthUser authUser, HttpServletRequest request) {
        if (authUser != null) {
            return;
        }
        Object error = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (error instanceof BaseErrorCode errorCode) {
            throw new BusinessException(errorCode);
        }
        throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
    }
}
