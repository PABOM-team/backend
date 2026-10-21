package com.pabom.backend.pet.infrastructure.scheduling;

import com.pabom.backend.pet.application.service.PetCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetCleanupScheduler {

    private final PetCleanupService cleanupService;

    @Scheduled(
            fixedDelayString = "${app.pet.cleanup-interval-ms:300000}",
            initialDelayString = "${app.pet.cleanup-interval-ms:300000}"
    )
    public void cleanUpDeletedPets() {
        cleanupService.processDuePets();
    }
}
