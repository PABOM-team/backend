package com.pabom.backend.notification.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BaseErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import com.pabom.backend.notification.application.service.PushTestService;
import com.pabom.backend.notification.presentation.docs.PushTestControllerDocs;
import com.pabom.backend.notification.presentation.response.PushTestResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 내 기기로 테스트 푸시 보내기. 운영(prod)에는 등록되지 않는다. */
@Profile({"local", "dev"})
@RestController
@RequestMapping(value = "/api/v1/push-tokens/test", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PushTestController implements PushTestControllerDocs {

    private final PushTestService pushTestService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponseBody<PushTestResponse>> sendTest(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(
                PushTestResponse.from(pushTestService.sendToMyDevices(authUser.userId())), request));
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
