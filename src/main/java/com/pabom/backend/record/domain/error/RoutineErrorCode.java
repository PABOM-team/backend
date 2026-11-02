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
            "완료 또는 건너뛰기 기록이 있는 루틴의 시작일은 변경할 수 없습니다."),
    ROUTINE_NOT_ACTIVE(HttpStatus.CONFLICT, "ROUTINE_NOT_ACTIVE", "비활성 루틴은 처리할 수 없습니다."),
    ROUTINE_ACTION_DUPLICATE(HttpStatus.CONFLICT, "ROUTINE_ACTION_DUPLICATE",
            "같은 날짜에 이미 완료 또는 건너뛰기 기록이 있습니다."),
    POSTPONE_DATE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "POSTPONE_DATE_INVALID",
            "미룰 날짜는 현재 예정일보다 뒤여야 합니다."),
    POSTPONEMENT_NOT_FOUND(HttpStatus.CONFLICT, "POSTPONEMENT_NOT_FOUND", "취소할 미루기가 없습니다."),
    POSTPONEMENT_CYCLE_COMPLETED(HttpStatus.CONFLICT, "POSTPONEMENT_CYCLE_COMPLETED",
            "완료된 회차의 미루기는 취소할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
