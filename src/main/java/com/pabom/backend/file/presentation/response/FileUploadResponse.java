package com.pabom.backend.file.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record FileUploadResponse(
        @Schema(description = "파일 ID", example = "301") Long id,
        @Schema(description = "파일 용도", example = "PET_PHOTO") FilePurpose purpose,
        @Schema(description = "검증된 MIME 타입", example = "image/jpeg") String contentType,
        @Schema(description = "파일 크기(byte)", example = "812345") long sizeBytes,
        @Schema(description = "이미지 너비(px)", example = "2048") int width,
        @Schema(description = "이미지 높이(px)", example = "1536") int height,
        @Schema(description = "10분간 유효한 비공개 S3 조회 URL", example = "https://storage.example.kr/...") String url,
        @Schema(description = "파일 상태", example = "TEMP") FileStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "TEMP 만료 시각", example = "2026-10-25T10:00:00+09:00") OffsetDateTime tempExpiresAt
) {
}
