package com.pabom.backend.careguideline.application.service;

import com.pabom.backend.careguideline.application.result.TodayEmptyReason;
import com.pabom.backend.careguideline.application.result.TodayResult;
import com.pabom.backend.careguideline.application.result.TodayTaskStatus;
import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import com.pabom.backend.careguideline.infrastructure.persistence.CareItemCodeMappingJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.pet.domain.type.PetStatus;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.CareRecord;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.domain.type.RecordType;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.infrastructure.persistence.SpeciesJpaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayQueryService {
    private static final Set<String> DATE_OVERRIDE_PROFILES =
            Set.of("local", "dev", "stage", "staging", "test", "ci");

    private final PetRepository pets;
    private final RoutineJpaRepository routines;
    private final CareRecordJpaRepository records;
    private final CareItemJpaRepository careItems;
    private final CareItemCodeMappingJpaRepository mappings;
    private final SpeciesJpaRepository species;
    private final RoutineDueDateCalculator dueDates;
    private final Clock clock;
    private final Environment environment;

    public TodayResult get(Long userId, Long petId, LocalDate requestedDate) {
        LocalDate date = resolveDate(requestedDate);
        List<Pet> ownedPets = ownedActivePets(userId, petId);
        if (ownedPets.isEmpty()) return empty(date, TodayEmptyReason.NO_PETS);

        Map<Long, Pet> petById = ownedPets.stream().collect(Collectors.toMap(Pet::getId, Function.identity()));
        List<Routine> activeRoutines = routines.findAllByPetIdInAndActiveTrue(petById.keySet()).stream()
                .filter(routine -> routine.getIntervalDays() != null)
                .toList();
        Map<Long, CareRecord> todayRecordByRoutine = new HashMap<>();
        Map<Long, CareRecord> latestRecordByRoutine = new HashMap<>();
        if (!activeRoutines.isEmpty()) {
            Collection<Long> routineIds = activeRoutines.stream().map(Routine::getId).toList();
            for (CareRecord record : records.findAllByUserIdAndRoutineIdInAndDeletedAtIsNull(userId, routineIds)) {
                latestRecordByRoutine.merge(record.getRoutineId(), record, this::laterRecord);
                if (dueDates.toServiceDate(record.getOccurredAt()).equals(date)) {
                    todayRecordByRoutine.merge(record.getRoutineId(), record, this::laterRecord);
                }
            }
        }

        ItemContext itemContext = itemContext(activeRoutines);
        Map<Long, Species> speciesById = species.findAllById(
                        ownedPets.stream().map(Pet::getSpeciesId).distinct().toList()).stream()
                .collect(Collectors.toMap(Species::getId, Function.identity()));
        Map<Long, List<TodayResult.Task>> tasksByPet = new HashMap<>();
        TodayResult.NextUpcoming nextUpcoming = null;
        LocalDate nextUpcomingDate = null;

        for (Routine routine : activeRoutines) {
            CareItem item = itemContext.item(routine);
            if (item == null) continue;
            String code = itemContext.code(routine, item);
            CareRecord latestRecord = latestRecordByRoutine.get(routine.getId());
            LocalDate nextDueDate = routine.getNextDueAt() == null ? null
                    : dueDates.toServiceDate(routine.getNextDueAt());
            if (latestRecord != null) {
                LocalDate handledDate = dueDates.toServiceDate(latestRecord.getOccurredAt());
                if (nextDueDate == null || !nextDueDate.isAfter(handledDate)) {
                    nextDueDate = handledDate.plusDays(routine.getIntervalDays());
                }
            }
            if (nextDueDate != null && nextDueDate.isAfter(date)
                    && (nextUpcomingDate == null || nextDueDate.isBefore(nextUpcomingDate)
                    || nextDueDate.equals(nextUpcomingDate) && routine.getId() < nextUpcoming.routineId())) {
                nextUpcomingDate = nextDueDate;
                nextUpcoming = new TodayResult.NextUpcoming(routine.getId(), routine.getPetId(), code, nextDueDate);
            }

            CareRecord todayRecord = todayRecordByRoutine.get(routine.getId());
            TodayTaskStatus status;
            LocalDate taskDueDate;
            Integer overdueDays = null;
            if (todayRecord != null) {
                status = statusOf(todayRecord, item);
                taskDueDate = date;
            } else if (nextDueDate != null && !nextDueDate.isAfter(date)) {
                taskDueDate = nextDueDate;
                if (nextDueDate.isBefore(date)) {
                    status = TodayTaskStatus.OVERDUE;
                    overdueDays = Math.toIntExact(ChronoUnit.DAYS.between(nextDueDate, date));
                } else {
                    status = TodayTaskStatus.DUE;
                }
            } else {
                continue;
            }
            CareRecord last = todayRecord != null ? todayRecord : latestRecord;
            tasksByPet.computeIfAbsent(routine.getPetId(), ignored -> new ArrayList<>()).add(
                    new TodayResult.Task(routine.getId(), code, item.getName(), status, taskDueDate,
                            overdueDays, item.getGuidelineRecordType(), item.getUnit(), lastRecord(last, item)));
        }

        Comparator<TodayResult.Task> taskOrder = Comparator
                .comparingInt((TodayResult.Task task) -> statusOrder(task.status()))
                .thenComparing(TodayResult.Task::dueDate)
                .thenComparing(TodayResult.Task::routineId);
        List<TodayResult.PetTasks> grouped = new ArrayList<>();
        for (Pet pet : ownedPets) {
            List<TodayResult.Task> petTasks = tasksByPet.getOrDefault(pet.getId(), List.of()).stream()
                    .sorted(taskOrder).toList();
            if (petTasks.isEmpty()) continue;
            Species petSpecies = speciesById.get(pet.getSpeciesId());
            grouped.add(new TodayResult.PetTasks(pet.getId(), pet.getName(),
                    petSpecies == null ? null : petSpecies.getCommonName(),
                    pet.getPhotoFileId() == null ? null : new TodayResult.Photo(pet.getPhotoFileId()),
                    null, petTasks));
        }

        List<TodayResult.Task> allTasks = grouped.stream().flatMap(group -> group.tasks().stream()).toList();
        TodayResult.Summary summary = summary(allTasks);
        TodayEmptyReason emptyReason = allTasks.isEmpty() ? TodayEmptyReason.NOTHING_DUE
                : summary.dueCount() == 0 && summary.overdueCount() == 0 && summary.needsResultCount() == 0
                        ? TodayEmptyReason.ALL_DONE : null;
        return new TodayResult(date, summary, emptyReason, nextUpcoming, grouped);
    }

    private List<Pet> ownedActivePets(Long userId, Long petId) {
        if (petId != null) {
            Pet pet = pets.findByIdAndUserId(petId, userId)
                    .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
            if (pet.getStatus() != PetStatus.ACTIVE) throw new BusinessException(PetErrorCode.PET_NOT_FOUND);
            return List.of(pet);
        }
        return pets.findAllByUserId(userId).stream()
                .filter(pet -> pet.getStatus() == PetStatus.ACTIVE)
                .toList();
    }

    private LocalDate resolveDate(LocalDate requestedDate) {
        LocalDate today = LocalDate.now(clock.withZone(RoutineDueDateCalculator.SERVICE_ZONE));
        if (requestedDate == null) return today;
        boolean allowed = DATE_OVERRIDE_PROFILES.stream()
                .anyMatch(profile -> environment.acceptsProfiles(Profiles.of(profile)));
        return allowed ? requestedDate : today;
    }

    private ItemContext itemContext(List<Routine> routines) {
        List<CareItemCodeMapping> allMappings = mappings.findAll();
        Map<Long, CareItemCodeMapping> byLegacy = allMappings.stream()
                .collect(Collectors.toMap(CareItemCodeMapping::getLegacyCareItemId, Function.identity()));
        Set<Long> ids = routines.stream().map(routine -> {
            CareItemCodeMapping mapping = byLegacy.get(routine.getCareItemId());
            return mapping != null && mapping.getGuidelineCareItemId() != null
                    ? mapping.getGuidelineCareItemId() : routine.getCareItemId();
        }).collect(Collectors.toSet());
        Map<Long, CareItem> byId = careItems.findAllById(ids).stream()
                .collect(Collectors.toMap(CareItem::getId, Function.identity()));
        return new ItemContext(byLegacy, byId);
    }

    private TodayTaskStatus statusOf(CareRecord record, CareItem item) {
        if (record.getRecordType() == RecordType.SKIPPED) return TodayTaskStatus.SKIPPED;
        if (item.isOutcomeAllowed() && record.getOutcome() == null) return TodayTaskStatus.NEEDS_RESULT;
        return TodayTaskStatus.DONE;
    }

    private TodayResult.LastRecord lastRecord(CareRecord record, CareItem item) {
        if (record == null) return null;
        return new TodayResult.LastRecord(record.getId(), record.getRecordType(),
                OffsetDateTime.ofInstant(record.getOccurredAt(), RoutineDueDateCalculator.SERVICE_ZONE),
                record.getOutcome(), record.getValueNum(), item.getUnit());
    }

    private CareRecord laterRecord(CareRecord left, CareRecord right) {
        int time = left.getOccurredAt().compareTo(right.getOccurredAt());
        if (time != 0) return time > 0 ? left : right;
        return left.getId() > right.getId() ? left : right;
    }

    private int statusOrder(TodayTaskStatus status) {
        return switch (status) {
            case OVERDUE -> 0;
            case DUE, NEEDS_RESULT -> 1;
            case DONE, SKIPPED -> 2;
        };
    }

    private TodayResult.Summary summary(List<TodayResult.Task> tasks) {
        Map<TodayTaskStatus, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(TodayResult.Task::status, Collectors.counting()));
        return new TodayResult.Summary(count(counts, TodayTaskStatus.DUE),
                count(counts, TodayTaskStatus.OVERDUE), count(counts, TodayTaskStatus.NEEDS_RESULT),
                count(counts, TodayTaskStatus.DONE), count(counts, TodayTaskStatus.SKIPPED), 0);
    }

    private int count(Map<TodayTaskStatus, Long> counts, TodayTaskStatus status) {
        return Math.toIntExact(counts.getOrDefault(status, 0L));
    }

    private TodayResult empty(LocalDate date, TodayEmptyReason reason) {
        return new TodayResult(date, new TodayResult.Summary(0, 0, 0, 0, 0, 0),
                reason, null, List.of());
    }

    private record ItemContext(Map<Long, CareItemCodeMapping> byLegacy, Map<Long, CareItem> byId) {
        CareItem item(Routine routine) {
            CareItemCodeMapping mapping = byLegacy.get(routine.getCareItemId());
            Long id = mapping != null && mapping.getGuidelineCareItemId() != null
                    ? mapping.getGuidelineCareItemId() : routine.getCareItemId();
            return byId.get(id);
        }

        String code(Routine routine, CareItem item) {
            CareItemCodeMapping mapping = byLegacy.get(routine.getCareItemId());
            return mapping == null ? item.getCode() : mapping.getGuidelineItemCode();
        }
    }
}
