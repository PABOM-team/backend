package com.pabom.backend.notification.application.service;

import com.pabom.backend.notification.application.port.CareTaskQueryPort;
import com.pabom.backend.notification.application.port.CareTaskQueryPort.PetDueTasks;
import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.application.port.PushSenderPort.PushMessage;
import com.pabom.backend.notification.application.result.PushDeliveryResult;
import com.pabom.backend.notification.domain.entity.NotificationLog;
import com.pabom.backend.notification.domain.entity.NotificationSetting;
import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.repository.NotificationLogRepository;
import com.pabom.backend.notification.domain.repository.NotificationSettingRepository;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import com.pabom.backend.notification.domain.type.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 30분 주기로 실행되어, 회원 시간대 기준 알림 시각이 이번 구간에 들어오는 회원에게
 * 그날 예정된 케어 항목을 하루 1건으로 묶어 보낸다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareReminderService {

    static final String CARE_CHANNEL_ID = "care";
    static final String CARE_REMINDER_URL = "/home";

    private final PushSenderPort sender;
    private final PushDeliveryService deliveryService;
    private final PushTokenRepository pushTokenRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationLogRepository logRepository;
    private final CareTaskQueryPort careTaskQuery;
    private final CareReminderMessageFactory messageFactory;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public PushDeliveryResult sendDueReminders() {
        if (!sender.isConfigured()) {
            log.warn("Care reminder skipped: EXPO_ACCESS_TOKEN is not configured.");
            return PushDeliveryResult.empty();
        }
        Instant now = clock.instant();
        List<Long> userIds = pushTokenRepository.findDistinctUserIds();
        if (userIds.isEmpty()) {
            return PushDeliveryResult.empty();
        }
        Map<Long, NotificationSetting> settings = settingRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(NotificationSetting::getUserId, Function.identity()));

        List<Long> claimedUserIds = new ArrayList<>();
        Map<Long, CareReminderMessageFactory.Content> contents = new java.util.HashMap<>();
        for (Long userId : userIds) {
            NotificationSetting setting = settings.getOrDefault(userId, NotificationSetting.defaults(userId, now));
            try {
                if (!isDue(setting, now)) {
                    continue;
                }
                LocalDate targetDate = now.atZone(setting.zoneId()).toLocalDate();
                List<PetDueTasks> tasks = careTaskQuery.findDueTasks(userId, targetDate);
                if (tasks.isEmpty() || !claim(userId, targetDate, now)) {
                    continue;
                }
                contents.put(userId, messageFactory.create(tasks));
                claimedUserIds.add(userId);
            } catch (RuntimeException exception) {
                log.warn("Care reminder preparation failed. userId={}", userId, exception);
            }
        }
        if (claimedUserIds.isEmpty()) {
            return PushDeliveryResult.empty();
        }

        List<PushMessage> messages = new ArrayList<>();
        for (PushToken token : pushTokenRepository.findAllByUserIdIn(claimedUserIds)) {
            CareReminderMessageFactory.Content content = contents.get(token.getUserId());
            messages.add(new PushMessage(token.getToken(), content.title(), content.body(),
                    Map.of("url", CARE_REMINDER_URL), CARE_CHANNEL_ID));
        }
        PushDeliveryResult result = deliveryService.deliver(messages);
        log.info("Care reminders sent. users={} messages={} accepted={}",
                claimedUserIds.size(), result.targetCount(), result.acceptedCount());
        return result;
    }

    /** 회원 시간대의 현재 시각이 속한 30분 구간의 시작이 알림 시각과 같은지. */
    static boolean isDue(NotificationSetting setting, Instant now) {
        if (!setting.isCareReminderEnabled()) {
            return false;
        }
        ZonedDateTime local = now.atZone(setting.zoneId());
        int slotMinute = local.getMinute() / NotificationSetting.REMINDER_SLOT_MINUTES
                * NotificationSetting.REMINDER_SLOT_MINUTES;
        return LocalTime.of(local.getHour(), slotMinute).equals(setting.getCareReminderTime());
    }

    /** 같은 날 같은 회원에게 한 번만 보내도록 발송 기록을 먼저 남긴다. 서버가 여러 대여도 UNIQUE 제약이 막는다. */
    private boolean claim(Long userId, LocalDate targetDate, Instant now) {
        try {
            transactions.executeWithoutResult(status -> logRepository.saveAndFlush(
                    new NotificationLog(userId, NotificationType.CARE_REMINDER, targetDate, now)));
            return true;
        } catch (DataIntegrityViolationException alreadySent) {
            return false;
        }
    }
}
