package com.pabom.backend.record.presentation.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.pabom.backend.record.domain.type.FeedingOutcome;
import com.pabom.backend.record.domain.type.TimePrecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public class UpdateRecordRequest {
    @NotNull @Positive @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private Integer revisionNo;
    private OffsetDateTime occurredAt; private TimePrecision timePrecision; private FeedingOutcome outcome;
    private BigDecimal valueNum; private JsonNode detail;
    @Size(max = 500) private String memo; @Positive private Long photoFileId;
    private final Set<String> present = new HashSet<>();
    private final Set<String> notEditable = new HashSet<>();

    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer value) { revisionNo = value; present.add("revisionNo"); }
    @Schema(nullable = true, type = "string", format = "date-time") public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime value) { occurredAt = value; present.add("occurredAt"); }
    @Schema(nullable = true, allowableValues = {"EXACT", "DATE_ONLY"}) public TimePrecision getTimePrecision() { return timePrecision; }
    public void setTimePrecision(TimePrecision value) { timePrecision = value; present.add("timePrecision"); }
    @Schema(nullable = true, allowableValues = {"ATE_ALL", "ATE_PART", "REFUSED", "NOT_OBSERVED"}) public FeedingOutcome getOutcome() { return outcome; }
    public void setOutcome(FeedingOutcome value) { outcome = value; present.add("outcome"); }
    @Schema(nullable = true) public BigDecimal getValueNum() { return valueNum; }
    public void setValueNum(BigDecimal value) { valueNum = value; present.add("valueNum"); }
    @Schema(nullable = true) public JsonNode getDetail() { return detail; }
    public void setDetail(JsonNode value) { detail = value; present.add("detail"); }
    @Schema(nullable = true, maxLength = 500) public String getMemo() { return memo; }
    public void setMemo(String value) { memo = value; present.add("memo"); }
    @Schema(nullable = true) public Long getPhotoFileId() { return photoFileId; }
    public void setPhotoFileId(Long value) { photoFileId = value; present.add("photoFileId"); }
    @JsonAnySetter public void unknown(String name, JsonNode ignored) { notEditable.add(name); }
    @JsonIgnore
    public boolean has(String field) { return present.contains(field); }
    @JsonIgnore
    @Schema(hidden = true)
    public boolean hasNotEditableFields() { return !notEditable.isEmpty(); }
}
