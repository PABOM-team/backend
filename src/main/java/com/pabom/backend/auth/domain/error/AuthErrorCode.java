package com.pabom.backend.auth.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    UNSUPPORTED_OAUTH_PROVIDER(
            HttpStatus.BAD_REQUEST,
            "UNSUPPORTED_OAUTH_PROVIDER",
            "지원하지 않는 OAuth 제공자입니다."
    ),
    INVALID_SOCIAL_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_SOCIAL_TOKEN",
            "유효하지 않은 소셜 토큰입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
