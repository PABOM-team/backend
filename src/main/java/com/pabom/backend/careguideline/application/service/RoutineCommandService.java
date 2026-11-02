package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.careguideline.application.result.RoutineDetailsResult;
import com.pabom.backend.careguideline.application.result.RoutineMutationResult;
import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import com.pabom.backend.careguideline.domain.entity.CareStandard;
import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.infrastructure.persistence.CareGuidelineVersionJpaRepository;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.careguideline.infrastructure.persistence.CareStandardJpaRepository;
import com.pabom.backend.careguideline.presentation.request.UpdateRoutineRequest;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.pet.domain.type.PetStatus;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.CareRecord;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.domain.error.RecordErrorCode;
import com.pabom.backend.record.domain.error.RoutineErrorCode;
import com.pabom.backend.record.domain.type.CycleSource;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.species.domain.entity.LifeStage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutineCommandService {
    private final RoutineJpaRepository routines;
    private final PetRepository pets;
    private final CareRecordJpaRepository records;
    private final CareItemJpaRepository careItems;
    private final CareItemCodeMappingJpaRepository mappings;
    private final CareStandardJpaRepository standards;
    private final CareGuidelineVersionJpaRepository versions;
    private final ApplicableCareGuidelineService applicableGuidelines;
    private final RoutineDueDateCalculator dueDates;
    private final Clock clock;

    @Transactional
    public RoutineMutationResult update(Long userId, Long routineId, UpdateRoutineRequest request) {
        if (request.isEmpty() || request.hasUnknownFields()) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }
        if (request.has("intervalDays") && request.getIntervalDays() == null
                || request.has("isEnabled") && request.getIsEnabled() == null
                || request.has("startDate") && request.getStartDate() == null) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }

        Context context = ownedContext(userId, routineId);
        Routine routine = context.routine();
        Optional<CareRecord> lastRecord = records
                .findFirstByUserIdAndRoutineIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(userId, routineId);
        if (request.has("startDate") && lastRecord.isPresent()) {
            throw new BusinessException(RoutineErrorCode.ROUTINE_START_DATE_LOCKED);
        }

        RoutineMutationResult.Previous previous = previous(routine);
        int interval = request.has("intervalDays") ? request.getIntervalDays() : routine.getIntervalDays();
        boolean enabled = request.has("isEnabled") ? request.getIsEnabled() : routine.isActive();
        boolean intervalChanged = request.has("intervalDays")
                && !request.getIntervalDays().equals(routine.getIntervalDays());
        boolean enabling = request.has("isEnabled") && enabled && !routine.isActive();
        LocalDate startDate = routine.getStartDate();
        boolean customStartDate = routine.isStartDateCustom();
        Instant nextDueAt = routine.getNextDueAt();
        Instant now = clock.instant();

        if (request.has("startDate")) {
            startDate = request.getStartDate();
            customStartDate = true;
            nextDueAt = dueDates.toDateOnlyInstant(startDate);
        }
        if (intervalChanged) {
            if (lastRecord.isPresent()) {
                nextDueAt = dueDates.nextDueAfterCompletion(
                        dueDates.toServiceDate(lastRecord.get().getOccurredAt()), interval);
            } else if (customStartDate && startDate != null) {
                nextDueAt = dueDates.toDateOnlyInstant(startDate);
            } else {
                nextDueAt = dueDates.firstDueAt(context.item().getCode(), enabled, interval,
                        context.pet().getCreatedAt());
                startDate = nextDueAt == null ? null : dueDates.toServiceDate(nextDueAt);
            }
        }
        if (enabling && lastRecord.isEmpty() && !request.has("startDate") && !intervalChanged) {
            if (customStartDate && startDate != null) {
                nextDueAt = dueDates.toDateOnlyInstant(startDate);
            } else if (startDate == null) {
                nextDueAt = dueDates.firstDueAfterEnable(context.item().getCode(), interval, now);
                if (nextDueAt == null) {
                    nextDueAt = dueDates.firstDueAt(context.item().getCode(), true, interval,
                            context.pet().getCreatedAt());
                }
                startDate = nextDueAt == null ? null : dueDates.toServiceDate(nextDueAt);
            }
        }

        routine.customize(request.has("intervalDays") ? interval : null,
                request.has("isEnabled") ? enabled : null, startDate, customStartDate, nextDueAt,
                intervalChanged, request.has("reason") ? request.getReason() : routine.getModificationReason(),
                request.has("reasonNote") ? normalize(request.getReasonNote()) : routine.getModificationReasonNote(), now);
        routines.save(routine);
        return new RoutineMutationResult(details(context, routine, standardForStoredVersion(context, routine)), previous);
    }

    @Transactional
    public RoutineMutationResult reset(Long userId, Long routineId) {
        Context context = ownedContext(userId, routineId);
        Routine routine = context.routine();
        RoutineMutationResult.Previous previous = previous(routine);
        CareGuidelineVersion version = applicableGuidelines.requireApplicable();
        LifeStage stage = applicableGuidelines.guidelineStage(context.pet().getLifeStageId());
        CareStandard standard = standards
                .findByGuidelineVersionIdAndSpeciesIdAndLifeStageIdAndCareItemId(
                        version.getId(), context.pet().getSpeciesId(), stage.getId(), context.item().getId())
                .orElseThrow(() -> new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_NOT_FOUND));

        Instant initialDueAt = dueDates.firstDueAt(context.item().getCode(), standard.isDefaultEnabled(),
                standard.getDefaultCycleDays(), context.pet().getCreatedAt());
        LocalDate startDate = initialDueAt == null ? null : dueDates.toServiceDate(initialDueAt);
        Instant nextDueAt = records
                .findFirstByUserIdAndRoutineIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(userId, routineId)
                .map(record -> dueDates.nextDueAfterCompletion(
                        dueDates.toServiceDate(record.getOccurredAt()), standard.getDefaultCycleDays()))
                .orElse(initialDueAt);
        if (!standard.isDefaultEnabled()) nextDueAt = null;
        Instant now = clock.instant();
        routine.resetToGuideline(standard.getDefaultCycleDays(), standard.isDefaultEnabled(),
                version.getId(), startDate, nextDueAt, now);
        routines.save(routine);
        return new RoutineMutationResult(details(context, routine, standard), previous);
    }

    private Context ownedContext(Long userId, Long routineId) {
        Routine routine = routines.findById(routineId)
                .orElseThrow(() -> new BusinessException(RoutineErrorCode.ROUTINE_NOT_FOUND));
        Pet pet = pets.findActiveByIdAndUserIdForUpdate(routine.getPetId(), userId)
                .orElseThrow(() -> new BusinessException(RoutineErrorCode.ROUTINE_NOT_FOUND));
        if (pet.getStatus() != PetStatus.ACTIVE) throw new BusinessException(RecordErrorCode.PET_NOT_ACTIVE);
        CareItemCodeMapping mapping = mappings.findByLegacyCareItemId(routine.getCareItemId()).orElse(null);
        Long canonicalItemId = mapping != null && mapping.getGuidelineCareItemId() != null
                ? mapping.getGuidelineCareItemId() : routine.getCareItemId();
        CareItem item = careItems.findById(canonicalItemId)
                .orElseThrow(() -> new BusinessException(CareGuidelineQueryErrorCode.CARE_STANDARD_DATA_MISSING));
        String code = mapping == null ? item.getCode() : mapping.getGuidelineItemCode();
        return new Context(routine, pet, item, code);
    }

    private CareStandard standardForStoredVersion(Context context, Routine routine) {
        if (routine.getCycleSource() == CycleSource.USER_CUSTOM || routine.getGuidelineVersionId() == null) return null;
        LifeStage stage = applicableGuidelines.guidelineStage(context.pet().getLifeStageId());
        return standards.findByGuidelineVersionIdAndSpeciesIdAndLifeStageIdAndCareItemId(
                routine.getGuidelineVersionId(), context.pet().getSpeciesId(), stage.getId(), context.item().getId())
                .orElse(null);
    }

    private RoutineDetailsResult details(Context context, Routine routine, CareStandard standard) {
        boolean custom = routine.getCycleSource() == CycleSource.USER_CUSTOM;
        String versionLabel = routine.getGuidelineVersionId() == null ? null : versions
                .findById(routine.getGuidelineVersionId()).map(CareGuidelineVersion::getVersionLabel).orElse(null);
        Integer defaultDays = custom || standard == null ? null : standard.getDefaultCycleDays();
        Integer minDays = custom || standard == null ? null : standard.getMinDays();
        Integer maxDays = custom || standard == null ? null : standard.getMaxDays();
        Boolean outOfRange = custom || standard == null ? null
                : routine.getIntervalDays() < standard.getMinDays() || routine.getIntervalDays() > standard.getMaxDays();
        return new RoutineDetailsResult(routine.getId(), routine.getPetId(), context.code(), context.item().getName(),
                routine.getIntervalDays(), routine.isActive(), routine.getCycleSource(), routine.getStartDate(),
                offset(routine.getNextDueAt()), versionLabel, defaultDays, minDays, maxDays, outOfRange,
                routine.getModificationReason(), routine.getModificationReasonNote(), offset(routine.getUserModifiedAt()));
    }

    private RoutineMutationResult.Previous previous(Routine routine) {
        String version = routine.getGuidelineVersionId() == null ? null : versions
                .findById(routine.getGuidelineVersionId()).map(CareGuidelineVersion::getVersionLabel).orElse(null);
        return new RoutineMutationResult.Previous(routine.getIntervalDays(), routine.isActive(),
                routine.getStartDate(), routine.getCycleSource(), version);
    }

    private OffsetDateTime offset(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, RoutineDueDateCalculator.SERVICE_ZONE);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Context(Routine routine, Pet pet, CareItem item, String code) {}
}
