package com.pabom.backend.record.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecordErrorCode implements BaseErrorCode {
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "기록을 찾을 수 없습니다."),
    PET_NOT_ACTIVE(HttpStatus.CONFLICT, "PET_NOT_ACTIVE", "활성 상태의 개체만 기록할 수 있습니다."),
    CLIENT_REQUEST_ID_REUSED(HttpStatus.CONFLICT, "CLIENT_REQUEST_ID_REUSED", "동일한 clientRequestId가 다른 요청에 사용되었습니다."),
    RECORD_CONFLICT(HttpStatus.CONFLICT, "RECORD_CONFLICT", "다른 요청에 의해 기록이 변경되었습니다."),
    RECORD_DELETED(HttpStatus.CONFLICT, "RECORD_DELETED", "삭제된 기록은 수정할 수 없습니다."),
    RECORD_NOT_DELETED(HttpStatus.CONFLICT, "RECORD_NOT_DELETED", "삭제된 기록만 복구할 수 있습니다."),
    RESTORE_WINDOW_EXPIRED(HttpStatus.GONE, "RESTORE_WINDOW_EXPIRED", "기록 복구 가능 기간이 지났습니다."),
    CARE_ITEM_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "CARE_ITEM_INVALID", "유효한 관리 항목이 아닙니다."),
    OUTCOME_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "OUTCOME_NOT_ALLOWED", "이 관리 항목에는 급여 결과를 입력할 수 없습니다."),
    VALUE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "VALUE_REQUIRED", "측정 항목에는 측정값이 필요합니다."),
    VALUE_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "VALUE_OUT_OF_RANGE", "측정값이 허용 범위를 벗어났습니다."),
    MEMO_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "MEMO_REQUIRED", "메모 항목에는 메모가 필요합니다."),
    OCCURRED_AT_IN_FUTURE(HttpStatus.UNPROCESSABLE_ENTITY, "OCCURRED_AT_IN_FUTURE", "기록 시각은 현재보다 5분을 초과해 미래일 수 없습니다."),
    OCCURRED_AT_TOO_OLD(HttpStatus.UNPROCESSABLE_ENTITY, "OCCURRED_AT_TOO_OLD", "기록 시각은 2000-01-01 이후여야 합니다."),
    MEMO_CONTACT_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "MEMO_CONTACT_NOT_ALLOWED", "메모와 상세에는 연락처를 입력할 수 없습니다."),
    FIELD_NOT_EDITABLE(HttpStatus.UNPROCESSABLE_ENTITY, "FIELD_NOT_EDITABLE", "수정할 수 없는 필드가 포함되어 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
