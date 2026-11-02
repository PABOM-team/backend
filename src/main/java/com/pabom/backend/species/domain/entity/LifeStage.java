package com.pabom.backend.species.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "life_stages")
public class LifeStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "has_own_guideline_rules", nullable = false)
    private boolean hasOwnGuidelineRules;

    @Column(name = "guideline_fallback_stage_id")
    private Long guidelineFallbackStageId;

    protected LifeStage() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean hasOwnGuidelineRules() {
        return hasOwnGuidelineRules;
    }

    public Long getGuidelineFallbackStageId() {
        return guidelineFallbackStageId;
    }

    public void updateGuidelineMetadata(
            String name,
            int displayOrder,
            boolean hasOwnGuidelineRules,
            Long guidelineFallbackStageId
    ) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.hasOwnGuidelineRules = hasOwnGuidelineRules;
        this.guidelineFallbackStageId = guidelineFallbackStageId;
    }
}
