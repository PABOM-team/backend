package com.pabom.backend.notification.presentation.response;

import com.pabom.backend.notification.application.result.PushDeliveryResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PushTestResponse(
        @Schema(description = "발송을 시도한 내 기기 토큰 수", example = "2")
        int targetCount,
        @Schema(description = "Expo가 접수한 건수(ticket status=ok)", example = "2")
        int acceptedCount
) {

    public static PushTestResponse from(PushDeliveryResult result) {
        return new PushTestResponse(result.targetCount(), result.acceptedCount());
    }
}
