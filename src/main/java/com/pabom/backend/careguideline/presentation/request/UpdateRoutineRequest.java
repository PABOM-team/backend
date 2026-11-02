package com.pabom.backend.careguideline.presentation.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.pabom.backend.record.domain.type.RoutineModificationReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class UpdateRoutineRequest {
    @Min(1) @Max(365)
    private Integer intervalDays;
    private Boolean isEnabled;
    private LocalDate startDate;
    private RoutineModificationReason reason;
    @Size(max = 100)
    private String reasonNote;
    private final Set<String> present = new HashSet<>();
    private final Set<String> unknown = new HashSet<>();

    public Integer getIntervalDays() { return intervalDays; }
    @JsonSetter("intervalDays") public void setIntervalDays(Integer value) { intervalDays = value; present.add("intervalDays"); }
    public Boolean getIsEnabled() { return isEnabled; }
    @JsonSetter("isEnabled") public void setIsEnabled(Boolean value) { isEnabled = value; present.add("isEnabled"); }
    @Schema(type = "string", format = "date") public LocalDate getStartDate() { return startDate; }
    @JsonSetter("startDate") public void setStartDate(LocalDate value) { startDate = value; present.add("startDate"); }
    public RoutineModificationReason getReason() { return reason; }
    @JsonSetter("reason") public void setReason(RoutineModificationReason value) { reason = value; present.add("reason"); }
    public String getReasonNote() { return reasonNote; }
    @JsonSetter("reasonNote") public void setReasonNote(String value) { reasonNote = value; present.add("reasonNote"); }
    @JsonAnySetter public void unknown(String name, JsonNode ignored) { unknown.add(name); }

    @JsonIgnore public boolean has(String field) { return present.contains(field); }
    @JsonIgnore public boolean isEmpty() { return present.isEmpty(); }
    @JsonIgnore public boolean hasUnknownFields() { return !unknown.isEmpty(); }
}
