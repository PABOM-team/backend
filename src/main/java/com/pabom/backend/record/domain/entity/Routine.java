package com.pabom.backend.record.domain.entity;

import com.pabom.backend.record.domain.type.CycleSource;
import com.pabom.backend.record.domain.type.RoutineModificationReason;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "routines")
public class Routine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "pet_id", nullable = false) private Long petId;
    @Column(name = "care_item_id", nullable = false) private Long careItemId;
    @Column(nullable = false) private boolean active;
    @Column(name = "interval_days") private Integer intervalDays;
    @Column(name = "next_due_at") private Instant nextDueAt;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "start_date_custom", nullable = false) private boolean startDateCustom;
    @Column(name = "guideline_version_id") private Long guidelineVersionId;
    @Enumerated(EnumType.STRING) @Column(name = "cycle_source", length = 20)
    private CycleSource cycleSource;
    @Enumerated(EnumType.STRING) @Column(name = "modification_reason", length = 30)
    private RoutineModificationReason modificationReason;
    @Column(name = "modification_reason_note", length = 100) private String modificationReasonNote;
    @Column(name = "user_modified_at") private Instant userModifiedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Routine() {}
    public static Routine active(Long petId, Long careItemId, Integer intervalDays, Instant now) {
        Routine routine = new Routine(); routine.petId = petId; routine.careItemId = careItemId;
        routine.active = true; routine.intervalDays = intervalDays; routine.updatedAt = now; return routine;
    }
    public static Routine guideline(Long petId, Long careItemId, int intervalDays, boolean active,
            Long guidelineVersionId, LocalDate startDate, Instant nextDueAt, Instant now) {
        Routine routine = new Routine();
        routine.petId = petId;
        routine.careItemId = careItemId;
        routine.intervalDays = intervalDays;
        routine.active = active;
        routine.guidelineVersionId = guidelineVersionId;
        routine.cycleSource = CycleSource.GUIDELINE;
        routine.startDate = startDate;
        routine.nextDueAt = nextDueAt;
        routine.updatedAt = now;
        return routine;
    }
    public void updateNextDueAt(Instant nextDueAt, Instant now) { this.nextDueAt = nextDueAt; this.updatedAt = now; }
    public void customize(Integer intervalDays, Boolean active, LocalDate startDate,
            boolean startDateCustom, Instant nextDueAt, boolean cycleCustomized,
            RoutineModificationReason reason, String reasonNote, Instant now) {
        if (intervalDays != null) this.intervalDays = intervalDays;
        if (active != null) this.active = active;
        this.startDate = startDate;
        this.startDateCustom = startDateCustom;
        this.nextDueAt = nextDueAt;
        if (cycleCustomized) this.cycleSource = CycleSource.USER_CUSTOM;
        this.modificationReason = reason;
        this.modificationReasonNote = reasonNote;
        this.userModifiedAt = now;
        this.updatedAt = now;
    }
    public void resetToGuideline(int intervalDays, boolean active, Long guidelineVersionId,
            LocalDate startDate, Instant nextDueAt, Instant now) {
        this.intervalDays = intervalDays;
        this.active = active;
        this.guidelineVersionId = guidelineVersionId;
        this.cycleSource = CycleSource.GUIDELINE;
        this.startDate = startDate;
        this.startDateCustom = false;
        this.nextDueAt = nextDueAt;
        this.modificationReason = null;
        this.modificationReasonNote = null;
        this.userModifiedAt = null;
        this.updatedAt = now;
    }
    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getCareItemId() { return careItemId; }
    public boolean isActive() { return active; }
    public Integer getIntervalDays() { return intervalDays; }
    public Instant getNextDueAt() { return nextDueAt; }
    public LocalDate getStartDate() { return startDate; }
    public boolean isStartDateCustom() { return startDateCustom; }
    public Long getGuidelineVersionId() { return guidelineVersionId; }
    public CycleSource getCycleSource() { return cycleSource; }
    public RoutineModificationReason getModificationReason() { return modificationReason; }
    public String getModificationReasonNote() { return modificationReasonNote; }
    public Instant getUserModifiedAt() { return userModifiedAt; }
}
