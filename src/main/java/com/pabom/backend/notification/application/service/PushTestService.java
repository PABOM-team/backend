package com.pabom.backend.notification.application.service;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.application.port.PushSenderPort.PushMessage;
import com.pabom.backend.notification.application.result.PushDeliveryResult;
import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.error.NotificationErrorCode;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 개발 환경에서 내 기기로 테스트 푸시를 보낸다. */
@Service
@RequiredArgsConstructor
public class PushTestService {

    static final String TITLE = "PABOM 테스트 알림";
    static final String BODY = "푸시 알림이 정상적으로 도착했어요.";

    private final PushSenderPort sender;
    private final PushDeliveryService deliveryService;
    private final PushTokenRepository pushTokenRepository;

    public PushDeliveryResult sendToMyDevices(Long userId) {
        if (!sender.isConfigured()) {
            throw new BusinessException(NotificationErrorCode.PUSH_NOT_CONFIGURED);
        }
        List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            throw new BusinessException(NotificationErrorCode.PUSH_TOKEN_NOT_FOUND);
        }
        List<PushMessage> messages = tokens.stream()
                .map(token -> new PushMessage(token.getToken(), TITLE, BODY,
                        Map.of("url", CareReminderService.CARE_REMINDER_URL),
                        CareReminderService.CARE_CHANNEL_ID))
                .toList();
        return deliveryService.deliver(messages);
    }
}
