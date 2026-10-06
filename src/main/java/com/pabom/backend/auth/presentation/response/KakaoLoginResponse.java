package com.pabom.backend.auth.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 OAuth 연동 테스트 결과")
public record KakaoLoginResponse(
        @Schema(description = "OAuth 제공자", example = "KAKAO")
        String provider,
        @Schema(description = "카카오 회원번호", example = "1234567890")
        String providerId,
        @Schema(description = "카카오 닉네임", example = "파봄")
        String nickname,
        @Schema(description = "카카오 계정 이메일", example = "pabom@example.com")
        String email
) {
}
