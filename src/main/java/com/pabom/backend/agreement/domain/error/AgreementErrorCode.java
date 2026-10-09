package com.pabom.backend.agreement.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AgreementErrorCode implements BaseErrorCode {
    REQUIRED_AGREEMENT_MISSING(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "REQUIRED_AGREEMENT_MISSING",
            "필수 약관에 모두 동의해야 합니다."
    ),
    AGREEMENT_VERSION_MISMATCH(
            HttpStatus.CONFLICT,
            "AGREEMENT_VERSION_MISMATCH",
            "약관 버전이 최신 버전과 일치하지 않습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
