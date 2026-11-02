package com.pabom.backend.careguideline.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.careguideline.application.result.*;
import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.entity.CareItemCodeMapping;
import com.pabom.backend.careguideline.infrastructure.persistence.*;
import com.pabom.backend.careguideline.presentation.request.*;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.pet.domain.type.PetStatus;
import com.pabom.backend.record.domain.entity.*;
import com.pabom.backend.record.domain.error.*;
import com.pabom.backend.record.domain.type.*;
import com.pabom.backend.record.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutineActionService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant MIN_OCCURRED_AT = LocalDate.of(2000, 1, 1).atStartOfDay(KST).toInstant();
    private static final Pattern CONTACT = Pattern.compile("(?i)(?:[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}|(?:\\+?82[- .]?)?0?1[016789][- .]?\\d{3,4}[- .]?\\d{4})");

    private final RoutineJpaRepository routines;
    private final CareRecordJpaRepository records;
    private final RoutinePostponementEventJpaRepository postponements;
    private final CareItemJpaRepository items;
    private final CareItemCodeMappingJpaRepository mappings;
    private final CareGuidelineVersionJpaRepository versions;
    private final PetRepository pets;
    private final RoutineDueDateCalculator dueDates;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public RoutineActionResult complete(Long userId, Long routineId, CompleteRoutineRequest request) {
        String hash = hash("COMPLETE", routineId, request);
        CareRecord replay = replay(userId, request.clientRequestId().toString(), hash);
        if (replay != null) return actionResult(replay, requireRoutine(routineId), statusOf(replay));

        Context context = ownedActiveContext(userId, routineId);
        replay = replay(userId, request.clientRequestId().toString(), hash);
        if (replay != null) return actionResult(replay, context.routine(), statusOf(replay));
        TimePrecision precision = request.timePrecision() == null ? TimePrecision.EXACT : request.timePrecision();
        Instant occurredAt = normalize(request.occurredAt(), precision);
        validateOccurredAt(occurredAt, precision);
        validateValues(context.item(), request.outcome(), request.valueNum(), request.detail(), request.memo());
        LocalDate actionDate = dueDates.toServiceDate(occurredAt);
        rejectDuplicate(routineId, actionDate);

        Instant now = clock.instant();
        CareRecord saved = saveAction(CareRecord.routineAction(userId, context.pet().getId(),
                context.item().getId(), routineId, request.clientRequestId().toString(), hash,
                RecordType.DONE, occurredAt, actionDate, precision, request.outcome(),
                request.valueNum(), json(request.detail()), clean(request.memo()), null,
                context.routine().getGuidelineVersionId(), context.versionLabel(), now));
        context.routine().finishOccurrence(actionDate, true,
                dueDates.nextDueAfterCompletion(actionDate, context.routine().getIntervalDays()), now);
        routines.save(context.routine());
        return actionResult(saved, context.routine(), statusOf(saved));
    }

    @Transactional
    public RoutineActionResult skip(Long userId, Long routineId, SkipRoutineRequest request) {
        String hash = hash("SKIP", routineId, request);
        CareRecord replay = replay(userId, request.clientRequestId().toString(), hash);
        if (replay != null) return actionResult(replay, requireRoutine(routineId), TodayTaskStatus.SKIPPED);

        Context context = ownedActiveContext(userId, routineId);
        replay = replay(userId, request.clientRequestId().toString(), hash);
        if (replay != null) return actionResult(replay, context.routine(), TodayTaskStatus.SKIPPED);
        LocalDate actionDate = request.occurredOn() == null ? LocalDate.now(clock.withZone(KST)) : request.occurredOn();
        if (actionDate.isAfter(LocalDate.now(clock.withZone(KST))))
            throw new BusinessException(RecordErrorCode.OCCURRED_AT_IN_FUTURE);
        if (actionDate.isBefore(MIN_OCCURRED_AT.atZone(KST).toLocalDate()))
            throw new BusinessException(RecordErrorCode.OCCURRED_AT_TOO_OLD);
        if (containsContact(request.memo()))
            throw new BusinessException(RecordErrorCode.MEMO_CONTACT_NOT_ALLOWED);
        rejectDuplicate(routineId, actionDate);

        Instant now = clock.instant();
        CareRecord saved = saveAction(CareRecord.routineAction(userId, context.pet().getId(),
                context.item().getId(), routineId, request.clientRequestId().toString(), hash,
                RecordType.SKIPPED, dueDates.toDateOnlyInstant(actionDate), actionDate,
                TimePrecision.DATE_ONLY, null, null, null, clean(request.memo()), request.reasonCode(),
                context.routine().getGuidelineVersionId(), context.versionLabel(), now));
        context.routine().finishOccurrence(actionDate, false,
                dueDates.nextDueAfterCompletion(actionDate, context.routine().getIntervalDays()), now);
        routines.save(context.routine());
        return actionResult(saved, context.routine(), TodayTaskStatus.SKIPPED);
    }

    @Transactional
    public RoutinePostponeResult postpone(Long userId, Long routineId, PostponeRoutineRequest request) {
        if (!request.isPostponeToSet()) throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        Context context = ownedActiveContext(userId, routineId);
        Routine routine = context.routine();
        Instant now = clock.instant();
        LocalDate currentDue = routine.getNextDueAt() == null ? null : dueDates.toServiceDate(routine.getNextDueAt());

        if (request.getPostponeTo() == null) {
            if (routine.getPostponedBaseDue() == null) {
                boolean completed = postponements.findFirstByRoutineIdOrderByIdDesc(routineId)
                        .filter(event -> event.getEventType() == PostponementEventType.POSTPONED)
                        .filter(event -> records.findFirstByUserIdAndRoutineIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(userId, routineId)
                                .map(record -> record.getCreatedAt().isAfter(event.getCreatedAt())).orElse(false))
                        .isPresent();
                throw new BusinessException(completed ? RoutineErrorCode.POSTPONEMENT_CYCLE_COMPLETED
                        : RoutineErrorCode.POSTPONEMENT_NOT_FOUND);
            }
            LocalDate base = routine.getPostponedBaseDue();
            postponements.save(RoutinePostponementEvent.of(routineId, currentDue, null, base,
                    PostponementEventType.CANCELLED, now));
            routine.cancelPostponement(dueDates.toDateOnlyInstant(base), now);
            routines.save(routine);
            return postponeResult(routine);
        }

        if (currentDue == null || !request.getPostponeTo().isAfter(currentDue))
            throw new BusinessException(RoutineErrorCode.POSTPONE_DATE_INVALID);
        LocalDate base = routine.getPostponedBaseDue() == null ? currentDue : routine.getPostponedBaseDue();
        postponements.save(RoutinePostponementEvent.of(routineId, currentDue, request.getPostponeTo(),
                base, PostponementEventType.POSTPONED, now));
        routine.postpone(base, dueDates.toDateOnlyInstant(request.getPostponeTo()), now);
        routines.save(routine);
        return postponeResult(routine);
    }

    private Context ownedActiveContext(Long userId, Long routineId) {
        Routine first = requireRoutine(routineId);
        Pet pet = pets.findActiveByIdAndUserIdForUpdate(first.getPetId(), userId)
                .orElseThrow(() -> new BusinessException(RoutineErrorCode.ROUTINE_NOT_FOUND));
        if (pet.getStatus() != PetStatus.ACTIVE) throw new BusinessException(RecordErrorCode.PET_NOT_ACTIVE);
        Routine routine = routines.findByIdForUpdate(routineId)
                .orElseThrow(() -> new BusinessException(RoutineErrorCode.ROUTINE_NOT_FOUND));
        if (!routine.isActive() || routine.getIntervalDays() == null)
            throw new BusinessException(RoutineErrorCode.ROUTINE_NOT_ACTIVE);
        CareItemCodeMapping mapping = mappings.findByLegacyCareItemId(routine.getCareItemId()).orElse(null);
        Long itemId = mapping != null && mapping.getGuidelineCareItemId() != null
                ? mapping.getGuidelineCareItemId() : routine.getCareItemId();
        CareItem item = items.findById(itemId)
                .orElseThrow(() -> new BusinessException(RecordErrorCode.CARE_ITEM_INVALID));
        String versionLabel = routine.getGuidelineVersionId() == null ? null : versions
                .findById(routine.getGuidelineVersionId()).map(CareGuidelineVersion::getVersionLabel).orElse(null);
        return new Context(routine, pet, item, versionLabel);
    }

    private Routine requireRoutine(Long routineId) {
        return routines.findById(routineId)
                .orElseThrow(() -> new BusinessException(RoutineErrorCode.ROUTINE_NOT_FOUND));
    }
    private CareRecord replay(Long userId, String requestId, String hash) {
        return records.findByUserIdAndClientRequestId(userId, requestId).map(record -> {
            if (!record.getRequestHash().equals(hash))
                throw new BusinessException(RecordErrorCode.CLIENT_REQUEST_ID_REUSED);
            return record;
        }).orElse(null);
    }
    private void rejectDuplicate(Long routineId, LocalDate date) {
        if (records.findByRoutineIdAndActionDate(routineId, date).isPresent())
            throw new BusinessException(RoutineErrorCode.ROUTINE_ACTION_DUPLICATE);
    }
    private CareRecord saveAction(CareRecord record) {
        try { return records.saveAndFlush(record); }
        catch (DataIntegrityViolationException exception) {
            String message = exception.getMostSpecificCause().getMessage();
            if (message != null && message.toLowerCase().contains("uk_care_records_user_client_request"))
                throw new BusinessException(RecordErrorCode.CLIENT_REQUEST_ID_REUSED);
            throw new BusinessException(RoutineErrorCode.ROUTINE_ACTION_DUPLICATE);
        }
    }
    private Instant normalize(OffsetDateTime value, TimePrecision precision) {
        if (value == null) value = OffsetDateTime.now(clock);
        if (precision == TimePrecision.DATE_ONLY)
            return dueDates.toDateOnlyInstant(value.atZoneSameInstant(KST).toLocalDate());
        return value.toInstant();
    }
    private void validateOccurredAt(Instant value, TimePrecision precision) {
        if (value.isBefore(MIN_OCCURRED_AT)) throw new BusinessException(RecordErrorCode.OCCURRED_AT_TOO_OLD);
        if (precision == TimePrecision.DATE_ONLY) {
            if (dueDates.toServiceDate(value).isAfter(LocalDate.now(clock.withZone(KST))))
                throw new BusinessException(RecordErrorCode.OCCURRED_AT_IN_FUTURE);
        } else if (value.isAfter(clock.instant().plus(Duration.ofMinutes(5)))) {
            throw new BusinessException(RecordErrorCode.OCCURRED_AT_IN_FUTURE);
        }
    }
    private void validateValues(CareItem item, FeedingOutcome outcome, BigDecimal value,
            JsonNode detail, String memo) {
        if (outcome != null && !item.isOutcomeAllowed())
            throw new BusinessException(RecordErrorCode.OUTCOME_NOT_ALLOWED);
        if (item.getInputType() == CareItemInputType.NUMBER) {
            if (value == null) throw new BusinessException(RecordErrorCode.VALUE_REQUIRED);
            if (item.getMinValue() != null && value.compareTo(item.getMinValue()) < 0
                    || item.getMaxValue() != null && value.compareTo(item.getMaxValue()) > 0)
                throw new BusinessException(RecordErrorCode.VALUE_OUT_OF_RANGE);
        } else if (value != null) throw new BusinessException(RecordErrorCode.VALUE_OUT_OF_RANGE);
        if (item.getInputType() == CareItemInputType.MEMO && (memo == null || memo.isBlank()))
            throw new BusinessException(RecordErrorCode.MEMO_REQUIRED);
        if ("SHED".equals(item.getCode()) && detail != null && !detail.isNull()) {
            String stage = detail.path("stage").asText();
            if (!Set.of("STARTED", "FINISHED", "NOT_SURE").contains(stage)
                    || !detail.has("observedDirectly") || !detail.get("observedDirectly").isBoolean())
                throw new BusinessException(RecordErrorCode.CARE_ITEM_INVALID);
        }
        if (containsContact(memo) || containsContact(detail == null ? null : detail.toString()))
            throw new BusinessException(RecordErrorCode.MEMO_CONTACT_NOT_ALLOWED);
    }
    private boolean containsContact(String value) { return value != null && CONTACT.matcher(value).find(); }
    private TodayTaskStatus statusOf(CareRecord record) {
        if (record.getRecordType() == RecordType.SKIPPED) return TodayTaskStatus.SKIPPED;
        CareItem item = items.findById(record.getCareItemId()).orElseThrow();
        return item.isOutcomeAllowed() && record.getOutcome() == null
                ? TodayTaskStatus.NEEDS_RESULT : TodayTaskStatus.DONE;
    }
    private RoutineActionResult actionResult(CareRecord record, Routine routine, TodayTaskStatus status) {
        CareItem item = items.findById(record.getCareItemId())
                .orElseThrow(() -> new BusinessException(RecordErrorCode.CARE_ITEM_INVALID));
        return new RoutineActionResult(new RoutineActionResult.Record(record.getId(), record.getRecordType(),
                item.getCode(), OffsetDateTime.ofInstant(record.getOccurredAt(), KST), record.getTimePrecision(),
                record.getOutcome(), record.getValueNum(), item.getUnit(), record.getSkipReasonCode(), record.getMemo()),
                new RoutineActionResult.RoutineState(routine.getId(), routine.getLastDoneOn(),
                        routine.getNextDueAt() == null ? null : dueDates.toServiceDate(routine.getNextDueAt())), status);
    }
    private RoutinePostponeResult postponeResult(Routine routine) {
        LocalDate next = routine.getNextDueAt() == null ? null : dueDates.toServiceDate(routine.getNextDueAt());
        return new RoutinePostponeResult(new RoutinePostponeResult.RoutineState(routine.getId(), next,
                routine.getPostponedBaseDue(), routine.getPostponedBaseDue() == null ? null : next,
                routine.getPostponedAt() == null ? null : OffsetDateTime.ofInstant(routine.getPostponedAt(), KST)));
    }
    private String hash(String type, Long routineId, Object request) {
        try {
            String value = type + ":" + routineId + ":" + objectMapper.writeValueAsString(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private String json(JsonNode value) {
        if (value == null || value.isNull()) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new BusinessException(AuthErrorCode.VALIDATION_FAILED); }
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private record Context(Routine routine, Pet pet, CareItem item, String versionLabel) {}
}
