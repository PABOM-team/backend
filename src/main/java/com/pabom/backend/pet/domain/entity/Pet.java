package com.pabom.backend.pet.domain.entity;

import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.pet.domain.type.PetStatus;
import com.pabom.backend.species.domain.entity.Morph;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "pets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pets_user_client_request",
                columnNames = {"user_id", "client_request_id"}
        )
)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_request_id", nullable = false, length = 36)
    private String clientRequestId;

    @Column(name = "species_id", nullable = false)
    private Long speciesId;

    @Column(nullable = false, length = 20)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pet_morphs",
            joinColumns = @JoinColumn(name = "pet_id"),
            inverseJoinColumns = @JoinColumn(name = "morph_id")
    )
    private Set<Morph> morphs = new LinkedHashSet<>();

    @Column(name = "custom_morph", length = 40)
    private String customMorph;

    @Column(name = "hatched_at")
    private LocalDate hatchedAt;

    @Column(name = "adopted_at")
    private LocalDate adoptedAt;

    @Column(name = "life_stage_id")
    private Long lifeStageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_certainty", nullable = false, length = 20)
    private Certainty stageCertainty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex_certainty", nullable = false, length = 20)
    private Certainty sexCertainty;

    @Column(name = "photo_file_id")
    private Long photoFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pet() {
    }

    public static Pet create(
            Long userId,
            String clientRequestId,
            Long speciesId,
            String name,
            Collection<Morph> morphs,
            String customMorph,
            LocalDate hatchedAt,
            LocalDate adoptedAt,
            Long lifeStageId,
            Certainty stageCertainty,
            Sex sex,
            Certainty sexCertainty,
            Long photoFileId,
            Instant now
    ) {
        Pet pet = new Pet();
        pet.userId = userId;
        pet.clientRequestId = clientRequestId;
        pet.speciesId = speciesId;
        pet.name = name;
        pet.morphs.addAll(morphs);
        pet.customMorph = customMorph;
        pet.hatchedAt = hatchedAt;
        pet.adoptedAt = adoptedAt;
        pet.lifeStageId = lifeStageId;
        pet.stageCertainty = stageCertainty;
        pet.sex = sex;
        pet.sexCertainty = sexCertainty;
        pet.photoFileId = photoFileId;
        pet.status = PetStatus.ACTIVE;
        pet.createdAt = now;
        pet.updatedAt = now;
        return pet;
    }

    public void updateName(String name, Instant now) {
        this.name = name;
        touch(now);
    }

    public void updateMorphs(Collection<Morph> morphs, Instant now) {
        this.morphs.clear();
        this.morphs.addAll(morphs);
        touch(now);
    }

    public void updateCustomMorph(String customMorph, Instant now) {
        this.customMorph = customMorph;
        touch(now);
    }

    public void updateHatchedAt(LocalDate hatchedAt, Instant now) {
        this.hatchedAt = hatchedAt;
        touch(now);
    }

    public void updateAdoptedAt(LocalDate adoptedAt, Instant now) {
        this.adoptedAt = adoptedAt;
        touch(now);
    }

    public void updateLifeStage(Long lifeStageId, Instant now) {
        this.lifeStageId = lifeStageId;
        touch(now);
    }

    public void updateStageCertainty(Certainty stageCertainty, Instant now) {
        this.stageCertainty = stageCertainty;
        touch(now);
    }

    public void updateSex(Sex sex, Instant now) {
        this.sex = sex;
        touch(now);
    }

    public void updateSexCertainty(Certainty sexCertainty, Instant now) {
        this.sexCertainty = sexCertainty;
        touch(now);
    }

    public void updatePhotoFileId(Long photoFileId, Instant now) {
        this.photoFileId = photoFileId;
        touch(now);
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getClientRequestId() { return clientRequestId; }
    public Long getSpeciesId() { return speciesId; }
    public String getName() { return name; }
    public List<Morph> getMorphs() {
        return morphs.stream().sorted(Comparator.comparingInt(Morph::getDisplayOrder)).toList();
    }
    public String getCustomMorph() { return customMorph; }
    public LocalDate getHatchedAt() { return hatchedAt; }
    public LocalDate getAdoptedAt() { return adoptedAt; }
    public Long getLifeStageId() { return lifeStageId; }
    public Certainty getStageCertainty() { return stageCertainty; }
    public Sex getSex() { return sex; }
    public Certainty getSexCertainty() { return sexCertainty; }
    public Long getPhotoFileId() { return photoFileId; }
    public PetStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
