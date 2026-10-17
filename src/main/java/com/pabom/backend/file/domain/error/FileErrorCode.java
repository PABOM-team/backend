package com.pabom.backend.file.domain.error;

import com.pabom.backend.global.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements BaseErrorCode {
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "파일 크기가 허용 범위를 초과했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_FILE_FORMAT", "유효한 이미지 파일이 아닙니다."),
    UNSUPPORTED_FILE_PURPOSE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FILE_PURPOSE", "현재 지원하지 않는 파일 용도입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_FAILED", "파일 저장 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
