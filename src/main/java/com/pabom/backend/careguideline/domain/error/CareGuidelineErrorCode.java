package com.pabom.backend.careguideline.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CareGuidelineErrorCode implements BaseErrorCode {
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다."),
    GUIDELINE_IMPORT_INVALID(HttpStatus.BAD_REQUEST, "GUIDELINE_IMPORT_INVALID", "가이드라인 CSV 검증에 실패했습니다."),
    GUIDELINE_VERSION_DUPLICATED(HttpStatus.CONFLICT, "GUIDELINE_VERSION_DUPLICATED", "이미 등록된 가이드라인 버전입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
