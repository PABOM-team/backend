package com.pabom.backend.notification.presentation.request;

import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPushTokenRequest(
        @Schema(description = "Expo 푸시 토큰. ExponentPushToken[...] 또는 ExpoPushToken[...]",
                example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        @NotBlank String token,
        @Schema(description = "기기 플랫폼", example = "IOS")
        @NotNull PushPlatform platform,
        @Schema(description = "앱 종류. 운영 앱(app.pabom.mobile)은 PRODUCTION, 개발 앱(app.pabom.mobile.dev)은 DEVELOPMENT",
                example = "PRODUCTION")
        @NotNull AppVariant appVariant
) {
}
