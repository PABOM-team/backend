package com.pabom.backend.careguideline.domain.entity;

import com.pabom.backend.careguideline.domain.type.GuidelineVersionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "care_guideline_versions")
public class CareGuidelineVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "version_label", nullable = false, unique = true, length = 100) private String versionLabel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private GuidelineVersionStatus status;
    @Column(name = "effective_from") private LocalDate effectiveFrom;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected CareGuidelineVersion() {}

    public static CareGuidelineVersion create(String versionLabel, boolean publish,
            LocalDate effectiveFrom, Long createdBy, Instant now) {
        CareGuidelineVersion version = new CareGuidelineVersion();
        version.versionLabel = versionLabel;
        version.status = publish ? GuidelineVersionStatus.PUBLISHED : GuidelineVersionStatus.DRAFT;
        version.effectiveFrom = effectiveFrom;
        version.publishedAt = publish ? now : null;
        version.createdBy = createdBy;
        version.createdAt = now;
        return version;
    }

    public Long getId() { return id; }
    public String getVersionLabel() { return versionLabel; }
    public GuidelineVersionStatus getStatus() { return status; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public Instant getPublishedAt() { return publishedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
