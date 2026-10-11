package com.pabom.backend.user.infrastructure.persistence;

import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return refreshTokenJpaRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHashForUpdate(tokenHash);
    }

    @Override
    public void revokeFamily(String familyId, Instant revokedAt) {
        refreshTokenJpaRepository.revokeFamily(familyId, revokedAt);
    }
}
