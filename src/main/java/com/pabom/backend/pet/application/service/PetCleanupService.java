package com.pabom.backend.pet.application.service;

import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordRevisionJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetCleanupService {

    private static final Duration RETENTION = Duration.ofDays(30);
    private static final int BATCH_SIZE = 50;

    private final PetJpaRepository pets;
    private final CareRecordJpaRepository records;
    private final CareRecordRevisionJpaRepository revisions;
    private final RoutineJpaRepository routines;
    private final FileJpaRepository files;
    private final FileStoragePort storage;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public void processDuePets() {
        Instant cutoff = clock.instant().minus(RETENTION);
        List<Long> petIds = pets.findCleanupCandidateIds(cutoff, PageRequest.of(0, BATCH_SIZE));
        petIds.forEach(this::process);
    }

    public void process(Long petId) {
        try {
            CleanupPlan plan = transactions.execute(status -> plan(petId));
            if (plan == null) {
                return;
            }
            plan.storageKeys().forEach(storage::delete);
            transactions.executeWithoutResult(status -> purge(petId));
        } catch (RuntimeException failure) {
            // The soft-deleted pet remains the durable retry marker. S3 DELETE is idempotent,
            // so a later scheduler run can safely repeat a partially completed cleanup.
            log.warn("Pet cleanup failed and will be retried. petId={}", petId, failure);
        }
    }

    private CleanupPlan plan(Long petId) {
        Pet pet = pets.findDeletedByIdForUpdate(petId).orElse(null);
        if (pet == null || pet.getDeletedAt().isAfter(clock.instant().minus(RETENTION))) {
            return null;
        }
        Set<Long> fileIds = exclusiveFileIds(pet);
        List<String> storageKeys = files.findAllById(fileIds).stream()
                .map(File::getStorageKey)
                .toList();
        return new CleanupPlan(storageKeys);
    }

    private void purge(Long petId) {
        Pet pet = pets.findDeletedByIdForUpdate(petId).orElse(null);
        if (pet == null || pet.getDeletedAt().isAfter(clock.instant().minus(RETENTION))) {
            return;
        }
        Set<Long> fileIds = exclusiveFileIds(pet);
        revisions.deleteAllByPetId(petId);
        records.deleteAllByPetId(petId);
        routines.deleteAllByPetId(petId);
        pets.deleteMorphLinksByPetId(petId);
        pets.deleteByPetId(petId);
        files.deleteAllByIdIn(fileIds);
    }

    private Set<Long> exclusiveFileIds(Pet pet) {
        Set<Long> candidates = new LinkedHashSet<>(records.findPhotoFileIdsByPetId(pet.getId()));
        if (pet.getPhotoFileId() != null) {
            candidates.add(pet.getPhotoFileId());
        }
        candidates.removeIf(fileId ->
                pets.countByPhotoFileIdAndIdNot(fileId, pet.getId()) > 0
                        || records.countByPhotoFileIdAndPetIdNot(fileId, pet.getId()) > 0);
        return candidates;
    }

    private record CleanupPlan(List<String> storageKeys) {
    }
}
