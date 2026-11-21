package com.pabom.backend.notification.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BaseErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import com.pabom.backend.notification.application.command.UpdateNotificationSettingCommand;
import com.pabom.backend.notification.application.service.NotificationSettingService;
import com.pabom.backend.notification.presentation.docs.NotificationSettingControllerDocs;
import com.pabom.backend.notification.presentation.request.UpdateNotificationSettingRequest;
import com.pabom.backend.notification.presentation.response.NotificationSettingResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/users/me/notification-settings", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NotificationSettingController implements NotificationSettingControllerDocs {

    private final NotificationSettingService settingService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponseBody<NotificationSettingResponse>> get(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(
                NotificationSettingResponse.from(settingService.get(authUser.userId())), request));
    }

    @Override
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseBody<NotificationSettingResponse>> update(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdateNotificationSettingRequest body,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser, request);
        UpdateNotificationSettingCommand command = new UpdateNotificationSettingCommand(
                body.careReminderEnabled(),
                body.careReminderTime() == null ? null : LocalTime.parse(body.careReminderTime()),
                body.timeZone()
        );
        return ResponseEntity.ok(ApiResponseBody.success(
                NotificationSettingResponse.from(settingService.update(authUser.userId(), command)), request));
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
