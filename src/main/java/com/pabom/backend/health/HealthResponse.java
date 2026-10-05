package com.pabom.backend.health;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서버 상태 확인 응답")
public record HealthResponse(
        @Schema(description = "애플리케이션과 필수 의존성의 종합 상태", example = "UP")
        String status
) {
}
