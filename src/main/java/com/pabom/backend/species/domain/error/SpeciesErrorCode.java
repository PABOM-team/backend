package com.pabom.backend.species.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SpeciesErrorCode implements BaseErrorCode {
    SPECIES_NOT_FOUND(HttpStatus.NOT_FOUND, "SPECIES_NOT_FOUND", "종을 찾을 수 없습니다."),
    LIFE_STAGE_INVALID(HttpStatus.BAD_REQUEST, "LIFE_STAGE_INVALID", "유효하지 않은 성장 단계입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
