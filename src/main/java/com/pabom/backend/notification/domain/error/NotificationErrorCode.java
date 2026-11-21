package com.pabom.backend.notification.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {
    INVALID_PUSH_TOKEN(
            HttpStatus.BAD_REQUEST,
            "INVALID_PUSH_TOKEN",
            "Expo 푸시 토큰 형식이 올바르지 않습니다."
    ),
    PUSH_TOKEN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PUSH_TOKEN_NOT_FOUND",
            "등록된 푸시 토큰이 없습니다."
    ),
    PUSH_NOT_CONFIGURED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "PUSH_NOT_CONFIGURED",
            "푸시 발송 설정이 되어 있지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
