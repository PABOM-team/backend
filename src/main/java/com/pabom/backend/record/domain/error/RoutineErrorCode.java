package com.pabom.backend.record.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoutineErrorCode implements BaseErrorCode {
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "관리 루틴을 찾을 수 없습니다."),
    ROUTINE_START_DATE_LOCKED(HttpStatus.CONFLICT, "ROUTINE_START_DATE_LOCKED",
            "완료 또는 건너뛰기 기록이 있는 루틴의 시작일은 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
