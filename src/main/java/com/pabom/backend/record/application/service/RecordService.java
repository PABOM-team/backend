package com.pabom.backend.record.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.type.PetStatus;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.record.domain.entity.*;
import com.pabom.backend.record.domain.error.RecordErrorCode;
import com.pabom.backend.record.domain.type.*;
import com.pabom.backend.record.infrastructure.persistence.*;
import com.pabom.backend.record.presentation.request.*;
import com.pabom.backend.record.presentation.response.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant MIN_OCCURRED_AT = LocalDate.of(2000, 1, 1).atStartOfDay(KST).toInstant();
    private static final Duration RESTORE_WINDOW = Duration.ofDays(30);
    private static final Pattern CONTACT = Pattern.compile("(?i)(?:[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}|(?:\\+?82[- .]?)?0?1[016789][- .]?\\d{3,4}[- .]?\\d{4})");

    private final CareRecordJpaRepository records;
    private final CareRecordRevisionJpaRepository revisions;
    private final CareItemJpaRepository items;
    private final RoutineJpaRepository routines;
    private final PetJpaRepository pets;
    private final FileJpaRepository files;
    private final FileStoragePort storage;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Transactional
    public RecordMutationResponse create(Long userId, CreateRecordRequest request) {
        Pet pet = ownedPetForUpdate(userId, request.petId());
        String hash = requestHash(request);
        Optional<CareRecord> replay = records.findByUserIdAndClientRequestId(userId, request.clientRequestId().toString());
        if (replay.isPresent()) {
            if (!replay.get().getRequestHash().equals(hash)) throw error(RecordErrorCode.CLIENT_REQUEST_ID_REUSED);
            Routine routine = replay.get().getRoutineId() == null ? null
                    : routines.findById(replay.get().getRoutineId()).orElse(null);
            return new RecordMutationResponse(toRecord(replay.get()), currentRoutine(routine));
        }
        if (pet.getStatus() != PetStatus.ACTIVE) throw error(RecordErrorCode.PET_NOT_ACTIVE);
        CareItem item = item(request.itemCode());
        TimePrecision precision = request.timePrecision() == null ? TimePrecision.EXACT : request.timePrecision();
        Instant occurredAt = normalize(request.occurredAt(), precision);
        validateOccurredAt(occurredAt, precision);
        validateValues(item, request.outcome(), request.valueNum(), request.detail(), request.memo());
        validatePhoto(userId, request.photoFileId());
        Routine routine = routines.findByPetIdAndCareItemIdAndActiveTrue(pet.getId(), item.getId()).orElse(null);
        Instant now = clock.instant();
        CareRecord saved = records.saveAndFlush(CareRecord.manual(userId, pet.getId(), item.getId(),
                routine == null ? null : routine.getId(), request.clientRequestId().toString(), hash,
                occurredAt, precision, request.outcome(), request.valueNum(), json(request.detail()),
                clean(request.memo()), request.photoFileId(), now));
        RoutineUpdatedResponse routineUpdated = recalculate(userId, routine, now);
        event("EV-12", "record_created_manual", userId, now);
        return new RecordMutationResponse(toRecord(saved), routineUpdated);
    }

    @Transactional(readOnly = true)
    public RecordDetailResponse get(Long userId, Long recordId) {
        CareRecord record = ownedRecord(userId, recordId);
        requireActiveParent(userId, record, false);
        Instant now = clock.instant();
        if (record.getDeletedAt() != null && record.getDeletedAt().plus(RESTORE_WINDOW).isBefore(now))
            throw error(RecordErrorCode.RECORD_NOT_FOUND);
        List<RevisionResponse> history = revisions.findAllByCareRecordIdOrderByIdAsc(recordId).stream()
                .map(this::toRevision).toList();
        return new RecordDetailResponse(toRecord(record), history);
    }

    @Transactional
    public RecordMutationResponse update(Long userId, Long recordId, UpdateRecordRequest request) {
        if (request.hasNotEditableFields()) throw error(RecordErrorCode.FIELD_NOT_EDITABLE);
        CareRecord record = ownedRecord(userId, recordId);
        requireActiveParent(userId, record, true);
        if (record.getDeletedAt() != null) throw error(RecordErrorCode.RECORD_DELETED);
        if (record.getRevisionNo() != request.getRevisionNo()) throw error(RecordErrorCode.RECORD_CONFLICT);
        CareItem item = items.findById(record.getCareItemId()).orElseThrow(() -> error(RecordErrorCode.CARE_ITEM_INVALID));
        TimePrecision precision = request.has("timePrecision") ? request.getTimePrecision() : record.getTimePrecision();
        if (precision == null) precision = TimePrecision.EXACT;
        OffsetDateTime requestedTime = request.has("occurredAt") ? request.getOccurredAt() : atKst(record.getOccurredAt());
        if (requestedTime == null) throw error(AuthErrorCode.VALIDATION_FAILED);
        Instant occurredAt = normalize(requestedTime, precision);
        FeedingOutcome outcome = request.has("outcome") ? request.getOutcome() : record.getOutcome();
        BigDecimal value = request.has("valueNum") ? request.getValueNum() : record.getValueNum();
        JsonNode detail = request.has("detail") ? request.getDetail() : parse(record.getDetailJson());
        String memo = request.has("memo") ? clean(request.getMemo()) : record.getMemo();
        Long photoId = request.has("photoFileId") ? request.getPhotoFileId() : record.getPhotoFileId();
        validateOccurredAt(occurredAt, precision); validateValues(item, outcome, value, detail, memo); validatePhoto(userId, photoId);

        ObjectNode before = snapshot(record);
        boolean firstResult = record.getOutcome() == null && outcome != null;
        Instant now = clock.instant();
        record.edit(occurredAt, precision, outcome, value, json(detail), memo, photoId, now);
        records.saveAndFlush(record);
        ObjectNode after = snapshot(record);
        List<String> changed = changed(before, after);
        revisions.save(CareRecordRevision.of(record.getId(), firstResult ? RevisionChangeType.RESULT_ADDED : RevisionChangeType.EDIT,
                json(objectMapper.valueToTree(changed)), json(before), json(after), now));
        Routine routine = record.getRoutineId() == null ? null : routines.findById(record.getRoutineId()).orElse(null);
        RoutineUpdatedResponse routineUpdated = recalculate(userId, routine, now);
        event("EV-15", "record_changed", userId, now);
        return new RecordMutationResponse(toRecord(record), routineUpdated);
    }

    @Transactional
    public DeleteRecordResponse delete(Long userId, Long recordId) {
        CareRecord record = ownedRecord(userId, recordId);
        requireActiveParent(userId, record, true);
        Routine routine = record.getRoutineId() == null ? null : routines.findById(record.getRoutineId()).orElse(null);
        if (record.getDeletedAt() != null) {
            return new DeleteRecordResponse(record.getId(), atKst(record.getDeletedAt()),
                    atKst(record.getDeletedAt().plus(RESTORE_WINDOW)), currentRoutine(routine));
        }
        Instant now = clock.instant(); ObjectNode before = snapshot(record);
        record.delete(now); records.saveAndFlush(record); ObjectNode after = snapshot(record);
        revisions.save(CareRecordRevision.of(record.getId(), RevisionChangeType.DELETE, "[\"deletedAt\"]",
                json(before), json(after), now));
        RoutineUpdatedResponse updated = recalculate(userId, routine, now);
        event("EV-15", "record_changed", userId, now);
        return new DeleteRecordResponse(record.getId(), atKst(now), atKst(now.plus(RESTORE_WINDOW)), updated);
    }

    @Transactional
    public RecordMutationResponse restore(Long userId, Long recordId) {
        CareRecord record = ownedRecord(userId, recordId);
        requireActiveParent(userId, record, true);
        if (record.getDeletedAt() == null) throw error(RecordErrorCode.RECORD_NOT_DELETED);
        Instant now = clock.instant();
        if (record.getDeletedAt().plus(RESTORE_WINDOW).isBefore(now)) throw error(RecordErrorCode.RESTORE_WINDOW_EXPIRED);
        Pet pet = ownedPetForUpdate(userId, record.getPetId());
        if (pet.getStatus() != PetStatus.ACTIVE) throw error(RecordErrorCode.PET_NOT_ACTIVE);
        ObjectNode before = snapshot(record); record.restore(now); records.saveAndFlush(record); ObjectNode after = snapshot(record);
        revisions.save(CareRecordRevision.of(record.getId(), RevisionChangeType.RESTORE, "[\"deletedAt\"]",
                json(before), json(after), now));
        Routine routine = record.getRoutineId() == null ? null : routines.findById(record.getRoutineId()).orElse(null);
        RoutineUpdatedResponse updated = recalculate(userId, routine, now);
        event("EV-15", "record_changed", userId, now);
        return new RecordMutationResponse(toRecord(record), updated);
    }

    @Transactional(readOnly = true)
    public RecordListResponse list(Long userId, Long petId, String itemCodes, RecordType recordType,
            LocalDate from, LocalDate to, boolean includeDeleted, String cursor, int size) {
        if (size < 1 || size > 50) throw error(AuthErrorCode.VALIDATION_FAILED);
        if (from != null && to != null && (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365))
            throw error(AuthErrorCode.VALIDATION_FAILED);
        if (petId != null) ownedPet(userId, petId);
        List<Long> activePetIds = pets.findActiveIdsByUserId(userId);
        List<Long> itemIds = null;
        if (itemCodes != null && !itemCodes.isBlank()) {
            itemIds = Arrays.stream(itemCodes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(code -> items.findByCodeAndActiveTrue(code.toUpperCase(Locale.ROOT))
                            .orElseThrow(() -> error(AuthErrorCode.VALIDATION_FAILED)))
                    .map(CareItem::getId).distinct().toList();
            if (itemIds.isEmpty()) throw error(AuthErrorCode.VALIDATION_FAILED);
        }
        Cursor decoded = cursor == null || cursor.isBlank() ? null : decodeCursor(cursor);
        final List<Long> filterItemIds = itemIds;
        Instant deletedCutoff = clock.instant().minus(RESTORE_WINDOW);
        Specification<CareRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(root.get("petId").in(activePetIds));
            if (petId != null) predicates.add(cb.equal(root.get("petId"), petId));
            if (filterItemIds != null) predicates.add(root.get("careItemId").in(filterItemIds));
            if (recordType != null) predicates.add(cb.equal(root.get("recordType"), recordType));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from.atStartOfDay(KST).toInstant()));
            if (to != null) predicates.add(cb.lessThan(root.get("occurredAt"), to.plusDays(1).atStartOfDay(KST).toInstant()));
            if (includeDeleted) predicates.add(cb.or(root.get("deletedAt").isNull(), cb.greaterThanOrEqualTo(root.get("deletedAt"), deletedCutoff)));
            else predicates.add(root.get("deletedAt").isNull());
            if (decoded != null) predicates.add(cb.or(
                    cb.lessThan(root.get("occurredAt"), decoded.occurredAt()),
                    cb.and(cb.equal(root.get("occurredAt"), decoded.occurredAt()), cb.lessThan(root.get("id"), decoded.id()))));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        List<CareRecord> found = records.findAll(spec, PageRequest.of(0, size + 1,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")))).getContent();
        boolean hasNext = found.size() > size;
        List<CareRecord> page = hasNext ? found.subList(0, size) : found;
        String next = hasNext ? encodeCursor(page.getLast()) : null;
        LastFeedingResponse lastFeeding = null;
        if (petId != null && decoded == null) {
            CareItem feeding = items.findByCodeAndActiveTrue("FEEDING").orElse(null);
            if (feeding != null) lastFeeding = records
                    .findFirstByUserIdAndPetIdAndCareItemIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(userId, petId, feeding.getId())
                    .map(r -> new LastFeedingResponse(petId, r.getId(), atKst(r.getOccurredAt()), r.getOutcome())).orElse(null);
        }
        return new RecordListResponse(page.stream().map(this::toListItem).toList(), next, lastFeeding);
    }

    private Pet ownedPet(Long userId, Long petId) {
        return pets.findOwnedById(petId, userId).orElseThrow(() -> error(PetErrorCode.PET_NOT_FOUND));
    }
    private Pet ownedPetForUpdate(Long userId, Long petId) {
        return pets.findActiveOwnedByIdForUpdate(petId, userId)
                .orElseThrow(() -> error(PetErrorCode.PET_NOT_FOUND));
    }
    private void requireActiveParent(Long userId, CareRecord record, boolean lock) {
        Optional<Pet> parent = lock
                ? pets.findActiveOwnedByIdForUpdate(record.getPetId(), userId)
                : pets.findOwnedById(record.getPetId(), userId);
        if (parent.isEmpty()) throw error(RecordErrorCode.RECORD_NOT_FOUND);
    }
    private CareRecord ownedRecord(Long userId, Long recordId) {
        return records.findByIdAndUserId(recordId, userId).orElseThrow(() -> error(RecordErrorCode.RECORD_NOT_FOUND));
    }
    private CareItem item(String code) {
        if (code == null) throw error(RecordErrorCode.CARE_ITEM_INVALID);
        return items.findByCodeAndActiveTrue(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> error(RecordErrorCode.CARE_ITEM_INVALID));
    }
    private void validateOccurredAt(Instant occurredAt, TimePrecision precision) {
        if (occurredAt.isBefore(MIN_OCCURRED_AT)) throw error(RecordErrorCode.OCCURRED_AT_TOO_OLD);
        if (precision == TimePrecision.DATE_ONLY) {
            if (occurredAt.atZone(KST).toLocalDate().isAfter(LocalDate.now(clock.withZone(KST))))
                throw error(RecordErrorCode.OCCURRED_AT_IN_FUTURE);
        } else if (occurredAt.isAfter(clock.instant().plus(Duration.ofMinutes(5)))) {
            throw error(RecordErrorCode.OCCURRED_AT_IN_FUTURE);
        }
    }
    private Instant normalize(OffsetDateTime value, TimePrecision precision) {
        if (precision == TimePrecision.DATE_ONLY) return value.atZoneSameInstant(KST).toLocalDate().atTime(12, 0).atZone(KST).toInstant();
        return value.toInstant();
    }
    private void validateValues(CareItem item, FeedingOutcome outcome, BigDecimal value, JsonNode detail, String memo) {
        if (outcome != null && !item.isOutcomeAllowed()) throw error(RecordErrorCode.OUTCOME_NOT_ALLOWED);
        if (item.getInputType() == CareItemInputType.NUMBER) {
            if (value == null) throw error(RecordErrorCode.VALUE_REQUIRED);
            if ((item.getMinValue() != null && value.compareTo(item.getMinValue()) < 0)
                    || (item.getMaxValue() != null && value.compareTo(item.getMaxValue()) > 0))
                throw error(RecordErrorCode.VALUE_OUT_OF_RANGE);
        } else if (value != null) throw error(RecordErrorCode.VALUE_OUT_OF_RANGE);
        if (item.getInputType() == CareItemInputType.MEMO && (memo == null || memo.isBlank()))
            throw error(RecordErrorCode.MEMO_REQUIRED);
        if ("SHED".equals(item.getCode()) && detail != null && !detail.isNull()) {
            String stage = detail.path("stage").asText();
            if (!Set.of("STARTED", "FINISHED", "NOT_SURE").contains(stage)
                    || !detail.has("observedDirectly") || !detail.get("observedDirectly").isBoolean())
                throw error(RecordErrorCode.CARE_ITEM_INVALID);
        }
        if (containsContact(memo) || containsContact(detail == null ? null : detail.toString()))
            throw error(RecordErrorCode.MEMO_CONTACT_NOT_ALLOWED);
    }
    private boolean containsContact(String value) { return value != null && CONTACT.matcher(value).find(); }
    private void validatePhoto(Long userId, Long photoFileId) {
        if (photoFileId == null) return;
        File file = files.findByIdAndOwnerIdAndStatusNot(photoFileId, userId, FileStatus.DELETED)
                .orElseThrow(() -> error(FileErrorCode.FILE_NOT_FOUND));
        if (file.getPurpose() != FilePurpose.RECORD_PHOTO) throw error(FileErrorCode.FILE_NOT_FOUND);
    }
    private RoutineUpdatedResponse recalculate(Long userId, Routine routine, Instant now) {
        if (routine == null) return null;
        Instant next = records.findFirstByUserIdAndRoutineIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(userId, routine.getId())
                .map(CareRecord::getOccurredAt).map(time -> routine.getIntervalDays() == null ? null
                        : time.plus(routine.getIntervalDays(), ChronoUnit.DAYS)).orElse(null);
        routine.updateNextDueAt(next, now); routines.save(routine);
        return new RoutineUpdatedResponse(routine.getId(), atKst(next));
    }
    private RoutineUpdatedResponse currentRoutine(Routine routine) {
        return routine == null ? null : new RoutineUpdatedResponse(routine.getId(), atKst(routine.getNextDueAt()));
    }
    private void event(String code, String name, Long userId, Instant now) {
        jdbc.update("insert into events(event_code,event_name,user_id,is_new_user,occurred_at) values (?,?,?,?,?)",
                code, name, userId, false, java.sql.Timestamp.from(now));
    }

    private RecordResponse toRecord(CareRecord record) {
        Pet pet = pets.findById(record.getPetId()).orElseThrow(() -> error(RecordErrorCode.RECORD_NOT_FOUND));
        CareItem item = items.findById(record.getCareItemId()).orElseThrow(() -> error(RecordErrorCode.CARE_ITEM_INVALID));
        PhotoResponse photo = null;
        if (record.getPhotoFileId() != null) {
            File file = files.findById(record.getPhotoFileId()).orElse(null);
            if (file != null && file.getStatus() != FileStatus.DELETED) {
                photo = new PhotoResponse(file.getId(), storage.createDownloadUrl(file.getStorageKey(), Duration.ofMinutes(10)));
            }
        }
        return new RecordResponse(record.getId(), record.getPetId(), pet.getName(), record.getRoutineId(),
                item.getCode(), item.getName(), record.getRecordType(), record.getSource(), atKst(record.getOccurredAt()),
                record.getTimePrecision(), atKst(record.getCreatedAt()), atKst(record.getUpdatedAt()), record.getOutcome(),
                record.getValueNum(), item.getUnit(), parse(record.getDetailJson()), record.getMemo(), photo,
                record.getStandardVersionLabel(), record.getRevisionNo(), atKst(record.getDeletedAt()),
                record.getDeletedAt() == null ? null : atKst(record.getDeletedAt().plus(RESTORE_WINDOW)));
    }
    private RecordListItemResponse toListItem(CareRecord record) {
        Pet pet = pets.findById(record.getPetId()).orElseThrow(() -> error(RecordErrorCode.RECORD_NOT_FOUND));
        CareItem item = items.findById(record.getCareItemId()).orElseThrow(() -> error(RecordErrorCode.CARE_ITEM_INVALID));
        String preview = preview(record.getMemo(), 40);
        return new RecordListItemResponse(record.getId(), record.getPetId(), pet.getName(), item.getCode(), item.getName(),
                record.getRecordType(), atKst(record.getOccurredAt()), record.getTimePrecision(), atKst(record.getCreatedAt()),
                record.getOutcome(), false, record.getValueNum(), item.getUnit(), summary(record, item), preview,
                record.getPhotoFileId() != null, record.getRevisionNo() > 1, record.getSource(), atKst(record.getDeletedAt()));
    }
    private String summary(CareRecord record, CareItem item) {
        if (record.getOutcome() != null) return switch (record.getOutcome()) {
            case ATE_ALL -> "모두 먹음"; case ATE_PART -> "일부 먹음";
            case REFUSED -> "먹지 않음"; case NOT_OBSERVED -> "확인하지 못함";
        };
        if (record.getValueNum() != null) return record.getValueNum().stripTrailingZeros().toPlainString()
                + (item.getUnit() == null ? "" : item.getUnit());
        JsonNode detail = parse(record.getDetailJson());
        if (detail != null && detail.hasNonNull("stage")) return detail.get("stage").asText();
        return record.getMemo() == null ? "" : preview(record.getMemo(), 40);
    }
    private String preview(String value, int maxCodePoints) {
        if (value == null) return null;
        int count = value.codePointCount(0, value.length());
        return count <= maxCodePoints ? value : value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
    private RevisionResponse toRevision(CareRecordRevision revision) {
        List<String> fields;
        try { fields = objectMapper.readerForListOf(String.class).readValue(revision.getChangedFieldsJson()); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        return new RevisionResponse(revision.getId(), revision.getChangeType(), fields,
                parse(revision.getBeforeJson()), parse(revision.getAfterJson()), atKst(revision.getCreatedAt()));
    }
    private ObjectNode snapshot(CareRecord r) {
        ObjectNode node = objectMapper.createObjectNode();
        put(node, "occurredAt", atKst(r.getOccurredAt())); put(node, "timePrecision", r.getTimePrecision());
        put(node, "outcome", r.getOutcome()); put(node, "valueNum", r.getValueNum());
        node.set("detail", parse(r.getDetailJson()) == null ? objectMapper.nullNode() : parse(r.getDetailJson()));
        put(node, "memo", r.getMemo()); put(node, "photoFileId", r.getPhotoFileId());
        put(node, "deletedAt", atKst(r.getDeletedAt()));
        return node;
    }
    private void put(ObjectNode node, String name, Object value) {
        node.set(name, value == null ? objectMapper.nullNode() : objectMapper.valueToTree(value));
    }
    private List<String> changed(ObjectNode before, ObjectNode after) {
        List<String> fields = new ArrayList<>();
        after.fieldNames().forEachRemaining(name -> { if (!Objects.equals(before.get(name), after.get(name))) fields.add(name); });
        return fields;
    }
    private String clean(String value) { return value == null ? null : value.trim(); }
    private String json(JsonNode value) { if (value == null || value.isNull()) return null; return value.toString(); }
    private JsonNode parse(String value) {
        if (value == null) return null;
        try { return objectMapper.readTree(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private String requestHash(CreateRecordRequest request) {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(request));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String encodeCursor(CareRecord record) {
        String raw = record.getOccurredAt() + "|" + record.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
    private Cursor decodeCursor(String value) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int separator = raw.lastIndexOf('|');
            return new Cursor(Instant.parse(raw.substring(0, separator)), Long.parseLong(raw.substring(separator + 1)));
        } catch (RuntimeException e) { throw error(AuthErrorCode.VALIDATION_FAILED); }
    }
    private OffsetDateTime atKst(Instant value) { return value == null ? null : value.atZone(KST).toOffsetDateTime(); }
    private BusinessException error(com.pabom.backend.global.error.BaseErrorCode code) { return new BusinessException(code); }
    private record Cursor(Instant occurredAt, long id) {}
}
