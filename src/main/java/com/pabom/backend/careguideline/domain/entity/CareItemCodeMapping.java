package com.pabom.backend.careguideline.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "care_item_code_mappings")
public class CareItemCodeMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "legacy_care_item_id", nullable = false, unique = true) private Long legacyCareItemId;
    @Column(name = "guideline_item_code", nullable = false, unique = true, length = 50) private String guidelineItemCode;
    @Column(name = "guideline_care_item_id", unique = true) private Long guidelineCareItemId;

    protected CareItemCodeMapping() {}

    public void linkGuidelineItem(Long guidelineCareItemId) {
        this.guidelineCareItemId = guidelineCareItemId;
    }

    public Long getId() { return id; }
    public Long getLegacyCareItemId() { return legacyCareItemId; }
    public String getGuidelineItemCode() { return guidelineItemCode; }
    public Long getGuidelineCareItemId() { return guidelineCareItemId; }
}
