package com.pabom.backend.auth.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 인가 페이지 URL")
public record KakaoAuthorizationUrlResponse(
        @Schema(
                description = "브라우저에서 열 카카오 로그인 URL",
                example = "https://kauth.kakao.com/oauth/authorize?client_id=...&redirect_uri=...&state=..."
        )
        String authorizationUrl
) {
}
