package com.pabom.backend.user.application.result;

import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import java.time.Instant;
import java.util.List;

public record UserProfileResult(
        Long id,
        String nickname,
        UserStatus status,
        UserRole role,
        Object region,
        List<AgreementItem> agreements,
        List<RequiredAgreementItem> requiredAgreements,
        boolean needsReconsent,
        PetCount petCount,
        Instant createdAt
) {
    public record AgreementItem(
            AgreementType type,
            String version,
            boolean agreed,
            Instant agreedAt
    ) {
    }

    public record RequiredAgreementItem(
            AgreementType type,
            String currentVersion,
            boolean required
    ) {
    }

    public record PetCount(long active, long ended) {
    }
}
