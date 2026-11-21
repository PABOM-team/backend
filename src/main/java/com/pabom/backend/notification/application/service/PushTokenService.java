package com.pabom.backend.notification.application.service;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.notification.application.command.RegisterPushTokenCommand;
import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.error.NotificationErrorCode;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final Clock clock;

    /** 멱등 등록. 같은 토큰이 다른 회원에게 있으면 요청한 회원으로 소유자를 옮긴다. */
    @Transactional
    public void register(Long userId, RegisterPushTokenCommand command) {
        if (!PushToken.isValidFormat(command.token())) {
            throw new BusinessException(NotificationErrorCode.INVALID_PUSH_TOKEN);
        }
        pushTokenRepository.upsert(
                userId,
                command.token(),
                command.platform(),
                command.appVariant(),
                clock.instant()
        );
    }

    /** 본인 토큰만 지운다. 없거나 다른 회원의 토큰이어도 성공으로 처리한다(멱등). */
    @Transactional
    public void unregister(Long userId, String token) {
        pushTokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
