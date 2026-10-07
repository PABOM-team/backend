package com.pabom.backend.auth.application.result;

import com.pabom.backend.auth.domain.model.OAuthProvider;

public record KakaoLoginResult(
        OAuthProvider provider,
        String providerId,
        String nickname
) {
}
