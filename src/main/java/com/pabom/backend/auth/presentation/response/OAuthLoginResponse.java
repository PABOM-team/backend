package com.pabom.backend.auth.presentation.response;

import com.pabom.backend.user.domain.type.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "OAuth 로그인 결과",
        oneOf = {OAuthLoginResponse.Signup.class, OAuthLoginResponse.Access.class}
)
public sealed interface OAuthLoginResponse
        permits OAuthLoginResponse.Signup, OAuthLoginResponse.Access {

    record Signup(
            @Schema(description = "가입 절차 전용 JWT") String signupToken,
            @Schema(description = "토큰 만료 시간(초)", example = "600") long expiresIn,
            @Schema(description = "이번 로그인에서 생성된 회원인지 여부") boolean isNewUser,
            UserInfo user
    ) implements OAuthLoginResponse {
    }

    record Access(
            @Schema(description = "서비스 Access Token") String accessToken,
            @Schema(description = "토큰 만료 시간(초)", example = "1800") long expiresIn,
            @Schema(description = "이번 로그인에서 생성된 회원인지 여부", example = "false")
            boolean isNewUser,
            UserInfo user
    ) implements OAuthLoginResponse {
    }

    record UserInfo(
            long id,
            String nickname,
            UserStatus status
    ) {
    }
}
