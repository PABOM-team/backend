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
    ),
    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "요청값이 누락되었거나 올바르지 않습니다."
    ),
    REDIRECT_URI_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "REDIRECT_URI_NOT_ALLOWED",
            "허용되지 않은 Redirect URI입니다."
    ),
    KAKAO_CODE_INVALID(
            HttpStatus.BAD_REQUEST,
            "KAKAO_CODE_INVALID",
            "카카오 인가 코드가 만료되었거나 유효하지 않습니다."
    ),
    KAKAO_UNAVAILABLE(
            HttpStatus.BAD_GATEWAY,
            "KAKAO_UNAVAILABLE",
            "카카오 OAuth 서버와 통신할 수 없습니다."
    ),
    GOOGLE_CODE_INVALID(
            HttpStatus.BAD_REQUEST,
            "GOOGLE_CODE_INVALID",
            "Google 인가 코드가 만료되었거나 유효하지 않습니다."
    ),
    GOOGLE_UNAVAILABLE(
            HttpStatus.BAD_GATEWAY,
            "GOOGLE_UNAVAILABLE",
            "Google OAuth 서버와 통신할 수 없습니다."
    ),
    RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "RATE_LIMITED",
            "로그인 요청 제한을 초과했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
