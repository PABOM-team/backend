package com.pabom.backend.notification.application.result;

public record PushDeliveryResult(int targetCount, int acceptedCount) {

    public static PushDeliveryResult empty() {
        return new PushDeliveryResult(0, 0);
    }
}
