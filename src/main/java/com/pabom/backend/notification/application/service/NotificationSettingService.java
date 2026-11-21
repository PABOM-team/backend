package com.pabom.backend.notification.application.service;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler.FieldErrorDetail;
import com.pabom.backend.notification.application.command.UpdateNotificationSettingCommand;
import com.pabom.backend.notification.application.result.NotificationSettingResult;
import com.pabom.backend.notification.domain.entity.NotificationSetting;
import com.pabom.backend.notification.domain.repository.NotificationSettingRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository settingRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public NotificationSettingResult get(Long userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> NotificationSetting.defaults(userId, clock.instant()));
        return NotificationSettingResult.from(setting);
    }

    @Transactional
    public NotificationSettingResult update(Long userId, UpdateNotificationSettingCommand command) {
        if (command.careReminderTime() != null
                && !NotificationSetting.isValidReminderTime(command.careReminderTime())) {
            throw validationFailed("careReminderTime", "HH:mm 형식이며 분은 00 또는 30이어야 합니다.");
        }
        if (command.timeZone() != null && !NotificationSetting.isValidTimeZone(command.timeZone())) {
            throw validationFailed("timeZone", "IANA 시간대 이름이어야 합니다. (예: Asia/Seoul)");
        }

        Instant now = clock.instant();
        NotificationSetting setting = settingRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> NotificationSetting.defaults(userId, now));
        setting.update(
                command.careReminderEnabled(),
                command.careReminderTime(),
                command.timeZone(),
                now
        );
        return NotificationSettingResult.from(settingRepository.save(setting));
    }

    private BusinessException validationFailed(String field, String message) {
        return new BusinessException(AuthErrorCode.VALIDATION_FAILED, new FieldErrorDetail(field, message));
    }
}
