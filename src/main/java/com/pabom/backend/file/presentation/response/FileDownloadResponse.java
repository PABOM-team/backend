package com.pabom.backend.file.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record FileDownloadResponse(
        @Schema(description = "10분간 유효한 비공개 S3 조회 URL", example = "https://storage.example.kr/...") String url,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "URL 만료 시각", example = "2026-10-24T10:10:00+09:00") OffsetDateTime expiresAt,
        @Schema(description = "검증된 MIME 타입", example = "image/jpeg") String contentType
) {
}
