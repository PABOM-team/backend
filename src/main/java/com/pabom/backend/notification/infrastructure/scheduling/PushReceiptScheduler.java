package com.pabom.backend.notification.infrastructure.scheduling;

import com.pabom.backend.notification.application.service.PushDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushReceiptScheduler {

    private final PushDeliveryService deliveryService;

    @Scheduled(
            fixedDelayString = "${app.push.receipt-check-interval-ms:900000}",
            initialDelayString = "${app.push.receipt-check-interval-ms:900000}"
    )
    public void checkReceipts() {
        deliveryService.checkReceipts();
    }
}
