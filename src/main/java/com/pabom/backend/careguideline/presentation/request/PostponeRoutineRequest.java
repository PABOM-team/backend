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

    @Schema(description = "미룰 날짜. 일반 미루기는 Asia/Seoul 기준 오늘보다 뒤이면서 오늘로부터 30일 미만이고, 현재 예정일보다 뒤여야 합니다. null은 기존 미루기 취소를 의미합니다.",
            example = "2026-11-25", type = "string", format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    public LocalDate getPostponeTo() { return postponeTo; }
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isPostponeToSet() { return postponeToSet; }
}
