package com.pabom.backend.agreement.application.result;

import com.pabom.backend.user.domain.type.UserStatus;

public record AgreementCompletionResult(
        long userId,
        UserStatus status,
        String accessToken,
        long expiresIn
) {
}
