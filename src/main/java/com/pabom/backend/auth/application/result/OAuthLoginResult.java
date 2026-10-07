package com.pabom.backend.auth.application.result;

import com.pabom.backend.auth.domain.model.OAuthProvider;

public record OAuthLoginResult(
        OAuthProvider provider,
        String providerId,
        String nickname
) {
}
