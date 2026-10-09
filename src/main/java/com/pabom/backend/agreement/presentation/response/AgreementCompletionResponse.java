package com.pabom.backend.agreement.presentation.response;

import com.pabom.backend.user.domain.type.UserStatus;

public record AgreementCompletionResponse(
        UserInfo user,
        String accessToken,
        long expiresIn
) {

    public record UserInfo(long id, UserStatus status) {
    }
}
