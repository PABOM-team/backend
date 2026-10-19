package com.pabom.backend.user.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    CONFIRM_REQUIRED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "CONFIRM_REQUIRED",
            "회원 탈퇴 확인이 필요합니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
