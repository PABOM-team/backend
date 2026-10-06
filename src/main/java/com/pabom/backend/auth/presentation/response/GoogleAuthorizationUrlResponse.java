package com.pabom.backend.auth.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구글 인가 페이지 URL")
public record GoogleAuthorizationUrlResponse(
        @Schema(
                description = "브라우저에서 열 구글 로그인 URL",
                example = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&state=..."
        )
        String authorizationUrl
) {
}
