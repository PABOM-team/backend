package com.pabom.backend.notification.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/** 바꿀 필드만 보낸다. 빠졌거나 null인 필드는 그대로 둔다. */
public record UpdateNotificationSettingRequest(
        @Schema(description = "케어 알림 수신 여부", example = "true", nullable = true)
        Boolean careReminderEnabled,
        @Schema(description = "케어 알림 시각(HH:mm). 분은 00 또는 30만 허용", example = "21:30", nullable = true)
        @Pattern(regexp = "^([01]\\d|2[0-3]):(00|30)$", message = "HH:mm 형식이며 분은 00 또는 30이어야 합니다.")
        String careReminderTime,
        @Schema(description = "IANA 시간대 이름", example = "Asia/Seoul", nullable = true)
        String timeZone
) {
}
