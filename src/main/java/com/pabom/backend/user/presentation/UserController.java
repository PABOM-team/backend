package com.pabom.backend.user.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.presentation.cookie.RefreshTokenCookieManager;
import com.pabom.backend.global.error.BaseErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import com.pabom.backend.user.application.service.UserProfileService;
import com.pabom.backend.user.application.service.WithdrawalService;
import com.pabom.backend.user.domain.error.UserErrorCode;
import com.pabom.backend.user.presentation.docs.UserControllerDocs;
import com.pabom.backend.user.presentation.request.WithdrawalRequest;
import com.pabom.backend.user.presentation.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserProfileService profileService;
    private final WithdrawalService withdrawalService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponseBody<UserProfileResponse>> getMe(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser, request);
        UserProfileResponse response = UserProfileResponse.from(
                profileService.get(authUser.userId()));
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody(required = false) WithdrawalRequest withdrawalRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        requireAuthentication(authUser, request);
        if (withdrawalRequest == null || !Boolean.TRUE.equals(withdrawalRequest.confirm())) {
            throw new BusinessException(UserErrorCode.CONFIRM_REQUIRED);
        }
        withdrawalService.withdraw(authUser.userId());
        refreshTokenCookieManager.clear(response);
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
