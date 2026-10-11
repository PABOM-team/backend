package com.pabom.backend.user.domain.repository;

import com.pabom.backend.user.domain.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    void revokeFamily(String familyId, Instant revokedAt);
}
