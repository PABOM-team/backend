package com.pabom.backend.record.presentation.response;

import com.pabom.backend.record.domain.type.FeedingOutcome;
import java.time.OffsetDateTime;

public record LastFeedingResponse(Long petId, Long recordId, OffsetDateTime occurredAt, FeedingOutcome outcome) {}
