package com.pabom.backend.record.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RecordListResponse(List<RecordListItemResponse> items,
        @Schema(nullable = true) String nextCursor,
        @Schema(nullable = true) LastFeedingResponse lastFeeding) {}
