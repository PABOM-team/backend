package com.pabom.backend.record.domain.entity;

import com.pabom.backend.record.domain.type.PostponementEventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "routine_postponement_events")
public class RoutinePostponementEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "routine_id", nullable = false) private Long routineId;
    @Column(name = "previous_due_on", nullable = false) private LocalDate previousDueOn;
    @Column(name = "new_due_on") private LocalDate newDueOn;
    @Column(name = "base_due_on", nullable = false) private LocalDate baseDueOn;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 20)
    private PostponementEventType eventType;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected RoutinePostponementEvent() {}

    public static RoutinePostponementEvent of(Long routineId, LocalDate previousDueOn,
            LocalDate newDueOn, LocalDate baseDueOn, PostponementEventType eventType, Instant now) {
        RoutinePostponementEvent event = new RoutinePostponementEvent();
        event.routineId = routineId;
        event.previousDueOn = previousDueOn;
        event.newDueOn = newDueOn;
        event.baseDueOn = baseDueOn;
        event.eventType = eventType;
        event.createdAt = now;
        return event;
    }

    public Long getId() { return id; }
    public Long getRoutineId() { return routineId; }
    public LocalDate getPreviousDueOn() { return previousDueOn; }
    public LocalDate getNewDueOn() { return newDueOn; }
    public LocalDate getBaseDueOn() { return baseDueOn; }
    public PostponementEventType getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
}
