package com.pabom.backend.record.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.pabom.backend.record.domain.type.RevisionChangeType;
import java.time.OffsetDateTime;
import java.util.List;

public record RevisionResponse(Long id, RevisionChangeType changeType, List<String> changedFields,
        JsonNode before, JsonNode after, OffsetDateTime createdAt) {}
