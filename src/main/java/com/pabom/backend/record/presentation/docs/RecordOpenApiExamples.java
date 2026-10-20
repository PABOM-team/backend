package com.pabom.backend.record.presentation.docs;

public final class RecordOpenApiExamples {
    private RecordOpenApiExamples() {}
    private static final String SUCCESS_PREFIX = "{\"resultType\":\"SUCCESS\",\"success\":{\"data\":";
    private static final String SUCCESS_SUFFIX = "},\"error\":null,\"meta\":{\"timestamp\":\"2026-10-20T11:00:00\",\"path\":\"/api/v1/records\"}}";
    public static final String CREATE_REQUEST = """
        {"clientRequestId":"a1b2c3d4-e5f6-47a8-9012-123456789abc","petId":12,"itemCode":"FEEDING","occurredAt":"2026-10-20T20:05:11+09:00","timePrecision":"EXACT","outcome":"ATE_PART","memo":"먹이를 일부 먹었어요.","photoFileId":null,"uxDurationMs":3500}
        """;
    public static final String UPDATE_REQUEST = """
        {"revisionNo":1,"occurredAt":"2026-10-19T20:05:00+09:00","memo":"날짜를 수정했습니다."}
        """;
    public static final String LIST_SUCCESS = SUCCESS_PREFIX + """
        {"items":[{"id":902,"petId":12,"petName":"리플","itemCode":"FEEDING","itemName":"먹이 주기","recordType":"DONE","occurredAt":"2026-10-20T20:05:11+09:00","timePrecision":"EXACT","createdAt":"2026-10-20T20:05:11+09:00","outcome":"ATE_PART","needsResult":false,"valueNum":null,"unit":null,"detailSummary":"일부 먹음","memoPreview":null,"hasPhoto":false,"isEdited":false,"source":"MANUAL","deletedAt":null}],"nextCursor":null,"lastFeeding":{"petId":12,"recordId":902,"occurredAt":"2026-10-20T20:05:11+09:00","outcome":"ATE_PART"}}
        """ + SUCCESS_SUFFIX;
    public static final String CREATE_SUCCESS = SUCCESS_PREFIX + """
        {"record":{"id":904,"petId":12,"petName":"리플","routineId":null,"itemCode":"WEIGHT","itemName":"체중","recordType":"DONE","source":"MANUAL","occurredAt":"2026-10-20T12:00:00+09:00","timePrecision":"DATE_ONLY","createdAt":"2026-10-20T20:05:11+09:00","updatedAt":"2026-10-20T20:05:11+09:00","outcome":null,"valueNum":42.5,"unit":"g","detail":null,"memo":null,"photo":null,"standardVersionLabel":null,"revisionNo":1,"deletedAt":null,"restorableUntil":null},"routineUpdated":null}
        """ + SUCCESS_SUFFIX;
    public static final String CREATE_WITH_ROUTINE_SUCCESS = SUCCESS_PREFIX + """
        {"record":{"id":905,"petId":12,"petName":"리플","routineId":33,"itemCode":"WEIGHT","itemName":"체중","recordType":"DONE","source":"MANUAL","occurredAt":"2026-10-20T12:00:00+09:00","timePrecision":"DATE_ONLY","createdAt":"2026-10-20T20:05:11+09:00","updatedAt":"2026-10-20T20:05:11+09:00","outcome":null,"valueNum":42.5,"unit":"g","detail":null,"memo":null,"photo":null,"standardVersionLabel":null,"revisionNo":1,"deletedAt":null,"restorableUntil":null},"routineUpdated":{"routineId":33,"nextDueAt":"2026-10-27T12:00:00+09:00"}}
        """ + SUCCESS_SUFFIX;
    public static final String DETAIL_SUCCESS = SUCCESS_PREFIX + """
        {"record":{"id":902,"petId":12,"petName":"리플","routineId":null,"itemCode":"FEEDING","itemName":"먹이 주기","recordType":"DONE","source":"MANUAL","occurredAt":"2026-10-19T20:05:00+09:00","timePrecision":"EXACT","createdAt":"2026-10-20T20:05:11+09:00","updatedAt":"2026-10-20T20:10:00+09:00","outcome":"ATE_PART","valueNum":null,"unit":null,"detail":null,"memo":null,"photo":null,"standardVersionLabel":null,"revisionNo":2,"deletedAt":null,"restorableUntil":null},"revisions":[{"id":51,"changeType":"EDIT","changedFields":["occurredAt"],"before":{"occurredAt":"2026-10-20T20:05:11+09:00"},"after":{"occurredAt":"2026-10-19T20:05:00+09:00"},"createdAt":"2026-10-20T20:10:00+09:00"}]}
        """ + SUCCESS_SUFFIX;
    public static final String UPDATE_SUCCESS = SUCCESS_PREFIX + """
        {"record":{"id":902,"petId":12,"petName":"리플","routineId":null,"itemCode":"FEEDING","itemName":"먹이 주기","recordType":"DONE","source":"MANUAL","occurredAt":"2026-10-19T20:05:00+09:00","timePrecision":"EXACT","createdAt":"2026-10-20T20:05:11+09:00","updatedAt":"2026-10-20T20:10:00+09:00","outcome":"ATE_PART","valueNum":null,"unit":null,"detail":null,"memo":"날짜를 수정했습니다.","photo":null,"standardVersionLabel":null,"revisionNo":2,"deletedAt":null,"restorableUntil":null},"routineUpdated":null}
        """ + SUCCESS_SUFFIX;
    public static final String DELETE_SUCCESS = SUCCESS_PREFIX + """
        {"recordId":902,"deletedAt":"2026-10-20T20:05:15+09:00","restorableUntil":"2026-11-19T20:05:15+09:00","routineUpdated":null}
        """ + SUCCESS_SUFFIX;
    public static final String RESTORE_SUCCESS = SUCCESS_PREFIX + """
        {"record":{"id":902,"petId":12,"petName":"리플","routineId":null,"itemCode":"FEEDING","itemName":"먹이 주기","recordType":"DONE","source":"MANUAL","occurredAt":"2026-10-19T20:05:00+09:00","timePrecision":"EXACT","createdAt":"2026-10-20T20:05:11+09:00","updatedAt":"2026-10-20T20:15:00+09:00","outcome":"ATE_PART","valueNum":null,"unit":null,"detail":null,"memo":null,"photo":null,"standardVersionLabel":null,"revisionNo":3,"deletedAt":null,"restorableUntil":null},"routineUpdated":null}
        """ + SUCCESS_SUFFIX;
    private static final String E1 = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"";
    private static final String E2 = "\",\"message\":\"";
    private static final String E3 = "\",\"details\":null},\"meta\":{\"timestamp\":\"2026-10-20T11:00:00\",\"path\":\"/api/v1/records\"}}";
    public static final String VALIDATION_FAILED = E1 + "VALIDATION_FAILED" + E2 + "요청값이 누락되었거나 올바르지 않습니다." + E3;
    public static final String AUTH_REQUIRED = E1 + "AUTH_REQUIRED" + E2 + "인증이 필요합니다." + E3;
    public static final String TOKEN_EXPIRED = E1 + "TOKEN_EXPIRED" + E2 + "Access Token이 만료되었습니다." + E3;
    public static final String TOKEN_INVALID = E1 + "TOKEN_INVALID" + E2 + "Access Token이 유효하지 않습니다." + E3;
    public static final String USER_WITHDRAWN = E1 + "USER_WITHDRAWN" + E2 + "탈퇴한 사용자입니다." + E3;
    public static final String PET_NOT_FOUND = E1 + "PET_NOT_FOUND" + E2 + "개체를 찾을 수 없습니다." + E3;
    public static final String FILE_NOT_FOUND = E1 + "FILE_NOT_FOUND" + E2 + "파일을 찾을 수 없습니다." + E3;
    public static final String RECORD_NOT_FOUND = E1 + "RECORD_NOT_FOUND" + E2 + "기록을 찾을 수 없습니다." + E3;
    public static final String PET_NOT_ACTIVE = E1 + "PET_NOT_ACTIVE" + E2 + "활성 상태의 개체만 기록할 수 있습니다." + E3;
    public static final String CLIENT_REQUEST_ID_REUSED = E1 + "CLIENT_REQUEST_ID_REUSED" + E2 + "동일한 clientRequestId가 다른 요청에 사용되었습니다." + E3;
    public static final String RECORD_CONFLICT = E1 + "RECORD_CONFLICT" + E2 + "다른 요청에 의해 기록이 변경되었습니다." + E3;
    public static final String RECORD_DELETED = E1 + "RECORD_DELETED" + E2 + "삭제된 기록은 수정할 수 없습니다." + E3;
    public static final String RECORD_NOT_DELETED = E1 + "RECORD_NOT_DELETED" + E2 + "삭제된 기록만 복구할 수 있습니다." + E3;
    public static final String RESTORE_WINDOW_EXPIRED = E1 + "RESTORE_WINDOW_EXPIRED" + E2 + "기록 복구 가능 기간이 지났습니다." + E3;
    public static final String CARE_ITEM_INVALID = E1 + "CARE_ITEM_INVALID" + E2 + "유효한 관리 항목이 아닙니다." + E3;
    public static final String OUTCOME_NOT_ALLOWED = E1 + "OUTCOME_NOT_ALLOWED" + E2 + "이 관리 항목에는 급여 결과를 입력할 수 없습니다." + E3;
    public static final String VALUE_REQUIRED = E1 + "VALUE_REQUIRED" + E2 + "측정 항목에는 측정값이 필요합니다." + E3;
    public static final String VALUE_OUT_OF_RANGE = E1 + "VALUE_OUT_OF_RANGE" + E2 + "측정값이 허용 범위를 벗어났습니다." + E3;
    public static final String MEMO_REQUIRED = E1 + "MEMO_REQUIRED" + E2 + "메모 항목에는 메모가 필요합니다." + E3;
    public static final String OCCURRED_AT_IN_FUTURE = E1 + "OCCURRED_AT_IN_FUTURE" + E2 + "기록 시각은 현재보다 5분을 초과해 미래일 수 없습니다." + E3;
    public static final String OCCURRED_AT_TOO_OLD = E1 + "OCCURRED_AT_TOO_OLD" + E2 + "기록 시각은 2000-01-01 이후여야 합니다." + E3;
    public static final String MEMO_CONTACT_NOT_ALLOWED = E1 + "MEMO_CONTACT_NOT_ALLOWED" + E2 + "메모와 상세에는 연락처를 입력할 수 없습니다." + E3;
    public static final String FIELD_NOT_EDITABLE = E1 + "FIELD_NOT_EDITABLE" + E2 + "수정할 수 없는 필드가 포함되어 있습니다." + E3;
}
