package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.port.ServiceTokenPort;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedRefreshToken;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedToken;
import com.pabom.backend.auth.application.result.TokenRefreshResult;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.exception.RefreshTokenReuseException;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.domain.repository.RefreshTokenRepository;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.UserStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthSessionService {

    private final ServiceTokenPort tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AuthSessionService(
            ServiceTokenPort tokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public TokenRefreshResult refresh(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new BusinessException(AuthErrorCode.REFRESH_INVALID);
        }

        Instant now = clock.instant();
        String tokenHash = tokenProvider.hashRefreshToken(rawRefreshToken);
        RefreshToken savedToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_INVALID));

        if (savedToken.isRotated()) {
            refreshTokenRepository.revokeFamily(savedToken.getFamilyId(), now);
            throw new RefreshTokenReuseException();
        }
        if (!savedToken.isActiveAt(now)) {
            throw new BusinessException(AuthErrorCode.REFRESH_INVALID);
        }

        Long tokenUserId = tokenProvider.validateRefreshToken(rawRefreshToken);
        if (!savedToken.getUserId().equals(tokenUserId)) {
            throw new BusinessException(AuthErrorCode.REFRESH_INVALID);
        }

        User user = userRepository.findById(tokenUserId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_INVALID));
        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.USER_WITHDRAWN);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.REFRESH_INVALID);
        }

        IssuedToken accessToken = tokenProvider.issueAccessToken(tokenUserId, now);
        IssuedRefreshToken refreshToken = tokenProvider.issueRefreshToken(
                tokenUserId,
                savedToken.getFamilyId(),
                now
        );
        savedToken.rotateTo(refreshToken.hash(), now);
        refreshTokenRepository.save(savedToken);
        refreshTokenRepository.save(new RefreshToken(
                refreshToken.hash(),
                tokenUserId,
                refreshToken.familyId(),
                refreshToken.issuedAt(),
                refreshToken.expiresAt()
        ));

        return new TokenRefreshResult(
                accessToken.value(),
                accessToken.expiresIn(),
                refreshToken.value(),
                refreshToken.expiresIn()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }
        String tokenHash = tokenProvider.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .ifPresent(token -> refreshTokenRepository.revokeFamily(
                        token.getFamilyId(),
                        clock.instant()
                ));
    }
}
