package com.pabom.backend.auth.application.port;

import java.time.Instant;

public interface ServiceTokenPort {

    IssuedToken issueAccessToken(Long userId, Instant issuedAt);

    IssuedToken issueSignupToken(Long userId, Instant issuedAt);

    IssuedRefreshToken issueRefreshToken(Long userId, Instant issuedAt);

    IssuedRefreshToken issueRefreshToken(Long userId, String familyId, Instant issuedAt);

    Long validateSignupToken(String token);

    Long validateAccessToken(String token);

    Long validateRefreshToken(String token);

    String hashRefreshToken(String token);

    record IssuedToken(String value, long expiresIn) {
    }

    record IssuedRefreshToken(
            String value,
            String hash,
            String familyId,
            Instant issuedAt,
            Instant expiresAt,
            long expiresIn
    ) {
    }
}
