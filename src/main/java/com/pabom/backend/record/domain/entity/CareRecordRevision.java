package com.pabom.backend.record.domain.entity;

import com.pabom.backend.record.domain.type.RevisionChangeType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "care_record_revisions")
public class CareRecordRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "care_record_id", nullable = false) private Long careRecordId;
    @Enumerated(EnumType.STRING) @Column(name = "change_type", nullable = false, length = 30)
    private RevisionChangeType changeType;
    @Column(name = "changed_fields_json", nullable = false, columnDefinition = "TEXT") private String changedFieldsJson;
    @Column(name = "before_json", nullable = false, columnDefinition = "TEXT") private String beforeJson;
    @Column(name = "after_json", nullable = false, columnDefinition = "TEXT") private String afterJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected CareRecordRevision() {}
    public static CareRecordRevision of(Long recordId, RevisionChangeType type, String fields,
            String before, String after, Instant now) {
        CareRecordRevision revision = new CareRecordRevision(); revision.careRecordId = recordId;
        revision.changeType = type; revision.changedFieldsJson = fields; revision.beforeJson = before;
        revision.afterJson = after; revision.createdAt = now; return revision;
    }
    public Long getId() { return id; } public Long getCareRecordId() { return careRecordId; }
    public RevisionChangeType getChangeType() { return changeType; }
    public String getChangedFieldsJson() { return changedFieldsJson; }
    public String getBeforeJson() { return beforeJson; } public String getAfterJson() { return afterJson; }
    public Instant getCreatedAt() { return createdAt; }
}
