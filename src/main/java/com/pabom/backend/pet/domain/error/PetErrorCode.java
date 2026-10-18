package com.pabom.backend.pet.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PetErrorCode implements BaseErrorCode {
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "PET_NOT_FOUND", "개체를 찾을 수 없습니다."),
    PET_NAME_INVALID(HttpStatus.BAD_REQUEST, "PET_NAME_INVALID", "개체 이름은 1자 이상 20자 이하여야 합니다."),
    MORPH_INVALID(HttpStatus.BAD_REQUEST, "MORPH_INVALID", "해당 종에서 선택할 수 없는 모프입니다."),
    MORPH_DUPLICATED(HttpStatus.BAD_REQUEST, "MORPH_DUPLICATED", "동일한 모프를 중복 선택할 수 없습니다."),
    MORPH_COMBINATION_INVALID(HttpStatus.BAD_REQUEST, "MORPH_COMBINATION_INVALID", "노멀 모프는 단독으로만 선택할 수 있습니다."),
    CUSTOM_MORPH_INVALID(HttpStatus.BAD_REQUEST, "CUSTOM_MORPH_INVALID", "기타 모프는 40자 이하여야 합니다."),
    HATCHED_AT_INVALID(HttpStatus.BAD_REQUEST, "HATCHED_AT_INVALID", "부화일은 미래일 수 없습니다."),
    PET_PHOTO_INVALID(HttpStatus.BAD_REQUEST, "PET_PHOTO_INVALID", "사용할 수 없는 개체 사진 파일입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
