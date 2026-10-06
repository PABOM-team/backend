package com.pabom.backend.auth.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구글 OAuth 연동 테스트 결과")
public record GoogleLoginResponse(
        @Schema(description = "OAuth 제공자", example = "GOOGLE")
        String provider,
        @Schema(description = "구글 회원 식별자", example = "1234567890")
        String providerId,
        @Schema(description = "구글 계정 이름", example = "파봄")
        String nickname,
        @Schema(description = "구글 계정 이메일", example = "pabom@example.com")
        String email
) {
}
