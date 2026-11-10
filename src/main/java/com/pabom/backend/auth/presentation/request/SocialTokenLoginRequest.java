package com.pabom.backend.auth.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "네이티브 SDK 소셜 토큰 로그인 요청. kakao는 accessToken, google은 idToken이 필요합니다.")
public record SocialTokenLoginRequest(
        @Schema(description = "카카오 SDK가 발급한 Access Token (kakao 전용)", example = "kakao-access-token")
        String accessToken,
        @Schema(description = "Google Sign-In SDK가 발급한 ID Token (google 전용)", example = "eyJhbGciOiJSUzI1NiIs...")
        String idToken
) {
}
