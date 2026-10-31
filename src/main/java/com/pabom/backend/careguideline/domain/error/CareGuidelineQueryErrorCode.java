package com.pabom.backend.careguideline.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CareGuidelineQueryErrorCode implements BaseErrorCode {
    APPLICABLE_GUIDELINE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICABLE_GUIDELINE_NOT_FOUND",
            "현재 적용 가능한 게시 가이드라인이 없습니다."),
    GUIDELINE_VERSION_CONFLICT(HttpStatus.CONFLICT, "GUIDELINE_VERSION_CONFLICT",
            "동일 적용일의 게시 가이드라인이 여러 개입니다."),
    CARE_STANDARD_DATA_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "CARE_STANDARD_DATA_MISSING",
            "가이드라인 관리 기준 데이터가 누락되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

