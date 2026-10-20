package com.pabom.backend.record.domain.entity;

import com.pabom.backend.record.domain.type.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "care_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_care_records_user_client_request", columnNames = {"user_id", "client_request_id"}))
public class CareRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "pet_id", nullable = false) private Long petId;
    @Column(name = "care_item_id", nullable = false) private Long careItemId;
    @Column(name = "routine_id") private Long routineId;
    @Column(name = "client_request_id", nullable = false, length = 36) private String clientRequestId;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Enumerated(EnumType.STRING) @Column(name = "record_type", nullable = false, length = 20) private RecordType recordType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RecordSource source;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Enumerated(EnumType.STRING) @Column(name = "time_precision", nullable = false, length = 20) private TimePrecision timePrecision;
    @Enumerated(EnumType.STRING) @Column(length = 30) private FeedingOutcome outcome;
    @Column(name = "value_num", precision = 19, scale = 4) private BigDecimal valueNum;
    @Column(name = "detail_json", columnDefinition = "TEXT") private String detailJson;
    @Column(length = 500) private String memo;
    @Column(name = "photo_file_id") private Long photoFileId;
    @Column(name = "standard_version_label", length = 100) private String standardVersionLabel;
    @Column(name = "revision_no", nullable = false) private int revisionNo;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CareRecord() {}

    public static CareRecord manual(Long userId, Long petId, Long itemId, Long routineId,
            String clientRequestId, String requestHash, Instant occurredAt, TimePrecision precision,
            FeedingOutcome outcome, BigDecimal valueNum, String detailJson, String memo,
            Long photoFileId, Instant now) {
        CareRecord record = new CareRecord();
        record.userId = userId; record.petId = petId; record.careItemId = itemId;
        record.routineId = routineId; record.clientRequestId = clientRequestId;
        record.requestHash = requestHash; record.recordType = RecordType.DONE;
        record.source = RecordSource.MANUAL; record.occurredAt = occurredAt;
        record.timePrecision = precision; record.outcome = outcome; record.valueNum = valueNum;
        record.detailJson = detailJson; record.memo = memo; record.photoFileId = photoFileId;
        record.revisionNo = 1; record.createdAt = now; record.updatedAt = now;
        return record;
    }

    public void edit(Instant occurredAt, TimePrecision precision, FeedingOutcome outcome,
            BigDecimal valueNum, String detailJson, String memo, Long photoFileId, Instant now) {
        this.occurredAt = occurredAt; this.timePrecision = precision; this.outcome = outcome;
        this.valueNum = valueNum; this.detailJson = detailJson; this.memo = memo;
        this.photoFileId = photoFileId; this.revisionNo++; this.updatedAt = now;
    }
    public void delete(Instant now) { if (deletedAt == null) { deletedAt = now; updatedAt = now; } }
    public void restore(Instant now) { deletedAt = null; revisionNo++; updatedAt = now; }

    public Long getId() { return id; } public Long getUserId() { return userId; }
    public Long getPetId() { return petId; } public Long getCareItemId() { return careItemId; }
    public Long getRoutineId() { return routineId; } public String getRequestHash() { return requestHash; }
    public RecordType getRecordType() { return recordType; } public RecordSource getSource() { return source; }
    public Instant getOccurredAt() { return occurredAt; } public TimePrecision getTimePrecision() { return timePrecision; }
    public FeedingOutcome getOutcome() { return outcome; } public BigDecimal getValueNum() { return valueNum; }
    public String getDetailJson() { return detailJson; } public String getMemo() { return memo; }
    public Long getPhotoFileId() { return photoFileId; } public String getStandardVersionLabel() { return standardVersionLabel; }
    public int getRevisionNo() { return revisionNo; } public Instant getDeletedAt() { return deletedAt; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
