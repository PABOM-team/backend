package com.pabom.backend.user.application.service;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.WithdrawalCleanupJob;
import com.pabom.backend.user.domain.repository.RefreshTokenRepository;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.infrastructure.persistence.WithdrawalCleanupJobJpaRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final WithdrawalCleanupJobJpaRepository cleanupJobRepository;
    private final Clock clock;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.TOKEN_INVALID));
        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.USER_WITHDRAWN);
        }

        Instant now = clock.instant();
        user.withdraw(now);
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId, now);
        cleanupJobRepository.save(WithdrawalCleanupJob.pending(userId, now));
    }
}
