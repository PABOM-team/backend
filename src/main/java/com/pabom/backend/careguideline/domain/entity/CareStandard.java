package com.pabom.backend.careguideline.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "care_standards", uniqueConstraints = @UniqueConstraint(
        name = "uk_care_standards_version_species_stage_item",
        columnNames = {"guideline_version_id", "species_id", "life_stage_id", "care_item_id"}))
public class CareStandard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "guideline_version_id", nullable = false) private Long guidelineVersionId;
    @Column(name = "species_id", nullable = false) private Long speciesId;
    @Column(name = "life_stage_id", nullable = false) private Long lifeStageId;
    @Column(name = "care_item_id", nullable = false) private Long careItemId;
    @Column(name = "default_cycle_days", nullable = false) private int defaultCycleDays;
    @Column(name = "min_days", nullable = false) private int minDays;
    @Column(name = "max_days", nullable = false) private int maxDays;
    @Column(name = "record_type", nullable = false, length = 20) private String recordType;
    @Column(name = "feed_result_set", length = 30) private String feedResultSet;
    @Column(name = "default_food_name", length = 100) private String defaultFoodName;
    @Column(name = "default_enabled", nullable = false) private boolean defaultEnabled;
    @Column(name = "evidence_level", nullable = false, length = 30) private String evidenceLevel;
    @Column(name = "evidence_source_label", nullable = false, length = 100) private String evidenceSourceLabel;
    @Column(name = "source_refs", nullable = false, columnDefinition = "TEXT") private String sourceRefs;

    protected CareStandard() {}

    public static CareStandard of(Long versionId, Long speciesId, Long stageId, Long itemId,
            int defaultCycleDays, int minDays, int maxDays, String recordType,
            String feedResultSet, String defaultFoodName, boolean defaultEnabled,
            String evidenceLevel, String evidenceSourceLabel, String sourceRefs) {
        CareStandard standard = new CareStandard();
        standard.guidelineVersionId = versionId; standard.speciesId = speciesId;
        standard.lifeStageId = stageId; standard.careItemId = itemId;
        standard.defaultCycleDays = defaultCycleDays; standard.minDays = minDays; standard.maxDays = maxDays;
        standard.recordType = recordType; standard.feedResultSet = feedResultSet;
        standard.defaultFoodName = defaultFoodName; standard.defaultEnabled = defaultEnabled;
        standard.evidenceLevel = evidenceLevel; standard.evidenceSourceLabel = evidenceSourceLabel;
        standard.sourceRefs = sourceRefs;
        return standard;
    }

    public Long getId() { return id; }
    public Long getGuidelineVersionId() { return guidelineVersionId; }
    public Long getSpeciesId() { return speciesId; }
    public Long getLifeStageId() { return lifeStageId; }
    public Long getCareItemId() { return careItemId; }
    public int getDefaultCycleDays() { return defaultCycleDays; }
    public int getMinDays() { return minDays; }
    public int getMaxDays() { return maxDays; }
    public String getRecordType() { return recordType; }
    public String getFeedResultSet() { return feedResultSet; }
    public String getDefaultFoodName() { return defaultFoodName; }
    public boolean isDefaultEnabled() { return defaultEnabled; }
    public String getEvidenceLevel() { return evidenceLevel; }
    public String getEvidenceSourceLabel() { return evidenceSourceLabel; }
    public String getSourceRefs() { return sourceRefs; }
}
