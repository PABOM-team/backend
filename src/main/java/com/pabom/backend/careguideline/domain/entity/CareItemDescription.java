package com.pabom.backend.careguideline.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "care_item_descriptions", uniqueConstraints = @UniqueConstraint(
        name = "uk_care_item_descriptions_version_key",
        columnNames = {"guideline_version_id", "description_key"}))
public class CareItemDescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "guideline_version_id", nullable = false) private Long guidelineVersionId;
    @Column(name = "description_key", nullable = false, length = 150) private String descriptionKey;
    @Column(name = "species_id") private Long speciesId;
    @Column(name = "care_item_id", nullable = false) private Long careItemId;
    @Column(name = "description_text", nullable = false, columnDefinition = "TEXT") private String descriptionText;

    protected CareItemDescription() {}

    public static CareItemDescription of(Long versionId, String descriptionKey, Long speciesId,
            Long careItemId, String descriptionText) {
        CareItemDescription description = new CareItemDescription();
        description.guidelineVersionId = versionId; description.descriptionKey = descriptionKey;
        description.speciesId = speciesId; description.careItemId = careItemId;
        description.descriptionText = descriptionText;
        return description;
    }

    public Long getId() { return id; }
    public Long getGuidelineVersionId() { return guidelineVersionId; }
    public String getDescriptionKey() { return descriptionKey; }
    public Long getSpeciesId() { return speciesId; }
    public Long getCareItemId() { return careItemId; }
    public String getDescriptionText() { return descriptionText; }
}
