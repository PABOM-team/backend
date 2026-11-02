package com.pabom.backend.careguideline.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "care_stage_thresholds", uniqueConstraints = @UniqueConstraint(
        name = "uk_care_stage_thresholds_version_species_sex",
        columnNames = {"guideline_version_id", "species_id", "sex"}))
public class CareStageThreshold {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "guideline_version_id", nullable = false) private Long guidelineVersionId;
    @Column(name = "species_id", nullable = false) private Long speciesId;
    @Column(nullable = false, length = 20) private String sex;
    @Column(name = "ref_weight_g", nullable = false, precision = 19, scale = 4) private BigDecimal refWeightG;
    @Column(name = "min_age_months", nullable = false) private int minAgeMonths;
    @Column(name = "hatch_adult_age_months", nullable = false) private int hatchAdultAgeMonths;

    protected CareStageThreshold() {}

    public static CareStageThreshold of(Long versionId, Long speciesId, String sex,
            BigDecimal refWeightG, int minAgeMonths, int hatchAdultAgeMonths) {
        CareStageThreshold threshold = new CareStageThreshold();
        threshold.guidelineVersionId = versionId; threshold.speciesId = speciesId;
        threshold.sex = sex; threshold.refWeightG = refWeightG;
        threshold.minAgeMonths = minAgeMonths; threshold.hatchAdultAgeMonths = hatchAdultAgeMonths;
        return threshold;
    }

    public Long getId() { return id; }
    public Long getGuidelineVersionId() { return guidelineVersionId; }
    public Long getSpeciesId() { return speciesId; }
    public String getSex() { return sex; }
    public BigDecimal getRefWeightG() { return refWeightG; }
    public int getMinAgeMonths() { return minAgeMonths; }
    public int getHatchAdultAgeMonths() { return hatchAdultAgeMonths; }
}
