package com.pabom.backend.record.presentation.response;

import java.util.List;

public record RecordDetailResponse(RecordResponse record, List<RevisionResponse> revisions) {}
