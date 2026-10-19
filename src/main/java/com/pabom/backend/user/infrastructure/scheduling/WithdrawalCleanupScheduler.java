package com.pabom.backend.user.infrastructure.scheduling;

import com.pabom.backend.user.application.service.WithdrawalCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawalCleanupScheduler {

    private final WithdrawalCleanupService cleanupService;

    @Scheduled(
            fixedDelayString = "${app.withdrawal.cleanup-interval-ms:300000}",
            initialDelayString = "${app.withdrawal.cleanup-interval-ms:300000}"
    )
    public void cleanUpWithdrawnUsers() {
        cleanupService.processDueJobs();
    }
}
