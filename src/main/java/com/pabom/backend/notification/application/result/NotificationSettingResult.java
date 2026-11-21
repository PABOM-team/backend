package com.pabom.backend.notification.application.result;

import com.pabom.backend.notification.domain.entity.NotificationSetting;
import java.time.LocalTime;

public record NotificationSettingResult(
        boolean careReminderEnabled,
        LocalTime careReminderTime,
        String timeZone
) {

    public static NotificationSettingResult from(NotificationSetting setting) {
        return new NotificationSettingResult(
                setting.isCareReminderEnabled(),
                setting.getCareReminderTime(),
                setting.getTimeZone()
        );
    }
}
