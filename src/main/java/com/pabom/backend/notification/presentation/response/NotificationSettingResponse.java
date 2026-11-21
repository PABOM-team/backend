package com.pabom.backend.notification.presentation.response;

import com.pabom.backend.notification.application.result.NotificationSettingResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;

public record NotificationSettingResponse(
        @Schema(description = "케어 알림 수신 여부", example = "true")
        boolean careReminderEnabled,
        @Schema(description = "케어 알림 시각(HH:mm, 30분 단위)", example = "09:00")
        String careReminderTime,
        @Schema(description = "IANA 시간대 이름", example = "Asia/Seoul")
        String timeZone
) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static NotificationSettingResponse from(NotificationSettingResult result) {
        return new NotificationSettingResponse(
                result.careReminderEnabled(),
                result.careReminderTime().format(TIME_FORMAT),
                result.timeZone()
        );
    }
}
