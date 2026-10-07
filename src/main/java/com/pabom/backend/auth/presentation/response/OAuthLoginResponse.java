package com.pabom.backend.auth.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OAuth 연동 테스트 결과")
public record OAuthLoginResponse(
        @Schema(description = "OAuth 제공자", example = "GOOGLE")
        String provider,
        @Schema(description = "OAuth 제공자의 회원 식별자", example = "1234567890")
        String providerId,
        @Schema(description = "OAuth 계정 이름 또는 닉네임", example = "파봄")
        String nickname
) {
}
