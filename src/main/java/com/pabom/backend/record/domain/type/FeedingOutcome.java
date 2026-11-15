package com.pabom.backend.record.domain.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "급여 결과. 크레스티드 게코의 DIET 급여는 EATING_TRACE_FOUND(먹은 흔적 있음), "
        + "EATING_TRACE_NOT_FOUND(먹은 흔적 없음), NOT_OBSERVED(관찰 못함)를 사용합니다. "
        + "그 외 급여는 ATE_ALL(전부 먹음), ATE_PART(일부 먹음), REFUSED(안 먹음), "
        + "NOT_OBSERVED(관찰 못함)를 사용합니다.")
public enum FeedingOutcome {
    ATE_ALL,
    ATE_PART,
    REFUSED,
    NOT_OBSERVED,
    EATING_TRACE_FOUND,
    EATING_TRACE_NOT_FOUND
}
