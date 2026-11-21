package com.pabom.backend.notification.application.command;

import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;

public record RegisterPushTokenCommand(String token, PushPlatform platform, AppVariant appVariant) {
}
