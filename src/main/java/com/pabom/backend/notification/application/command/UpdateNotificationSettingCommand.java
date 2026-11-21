package com.pabom.backend.notification.application.command;

import java.time.LocalTime;

/** null인 필드는 바꾸지 않는다. */
public record UpdateNotificationSettingCommand(
        Boolean careReminderEnabled,
        LocalTime careReminderTime,
        String timeZone
) {
}
