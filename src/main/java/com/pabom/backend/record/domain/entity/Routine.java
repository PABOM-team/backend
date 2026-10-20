package com.pabom.backend.record.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "routines")
public class Routine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "pet_id", nullable = false) private Long petId;
    @Column(name = "care_item_id", nullable = false) private Long careItemId;
    @Column(nullable = false) private boolean active;
    @Column(name = "interval_days") private Integer intervalDays;
    @Column(name = "next_due_at") private Instant nextDueAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Routine() {}
    public static Routine active(Long petId, Long careItemId, Integer intervalDays, Instant now) {
        Routine routine = new Routine(); routine.petId = petId; routine.careItemId = careItemId;
        routine.active = true; routine.intervalDays = intervalDays; routine.updatedAt = now; return routine;
    }
    public void updateNextDueAt(Instant nextDueAt, Instant now) { this.nextDueAt = nextDueAt; this.updatedAt = now; }
    public Long getId() { return id; }
    public Long getPetId() { return petId; }
    public Long getCareItemId() { return careItemId; }
    public boolean isActive() { return active; }
    public Integer getIntervalDays() { return intervalDays; }
    public Instant getNextDueAt() { return nextDueAt; }
}
