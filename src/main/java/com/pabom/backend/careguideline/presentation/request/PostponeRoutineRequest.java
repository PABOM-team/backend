package com.pabom.backend.careguideline.presentation.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public class PostponeRoutineRequest {
    private LocalDate postponeTo;
    private boolean postponeToSet;

    @JsonSetter(value = "postponeTo", nulls = Nulls.SET)
    public void setPostponeTo(LocalDate postponeTo) {
        this.postponeTo = postponeTo;
        this.postponeToSet = true;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    public LocalDate getPostponeTo() { return postponeTo; }
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isPostponeToSet() { return postponeToSet; }
}
