package com.pabom.backend.careguideline.presentation.docs;

public final class RoutineOpenApiExamples {
    private RoutineOpenApiExamples() {}

    public static final String TODAY_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"date":"2026-11-02","summary":{"dueCount":1,"overdueCount":1,"needsResultCount":0,"doneCount":0,"skippedCount":0,"reportCount":0},"emptyReason":null,"nextUpcoming":{"routineId":12,"petId":1,"itemCode":"CLEAN","dueDate":"2026-11-05"},"pets":[{"petId":1,"name":"레오","speciesCommonName":"레오파드 게코","photo":null,"stageNotice":null,"tasks":[{"routineId":10,"itemCode":"FEED","itemName":"급여","status":"OVERDUE","dueDate":"2026-11-01","originalDueDate":null,"overdueDays":1,"resultType":"FEED","unit":null,"lastRecord":null}]}]}},"error":null,"meta":{"timestamp":"2026-11-02T00:00:00","path":"/api/v1/today"}}
            """;
    public static final String UPDATE_REQUEST = """
            {"intervalDays":5,"isEnabled":true,"startDate":"2026-11-05","reason":"PREFERENCE","reasonNote":"관리 주기를 조정하고 싶어요","foodName":"귀뚜라미"}
            """;
    public static final String UPDATE_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"routine":{"id":10,"petId":1,"itemCode":"FEED","itemName":"급여","intervalDays":5,"isEnabled":true,"cycleSource":"USER_CUSTOM","startDate":"2026-11-05","nextDueAt":"2026-11-05T12:00:00+09:00","versionLabel":"1.2","defaultCycleDays":null,"minDays":null,"maxDays":null,"isOutOfRange":null,"reason":"PREFERENCE","reasonNote":"관리 주기를 조정하고 싶어요","modifiedAt":"2026-11-02T12:00:00+09:00","foodName":"귀뚜라미"},"previous":{"intervalDays":3,"isEnabled":true,"startDate":"2026-11-02","cycleSource":"GUIDELINE","versionLabel":"1.2"}}},"error":null,"meta":{"timestamp":"2026-11-02T03:00:00","path":"/api/v1/routines/10"}}
            """;
    public static final String RESET_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"routine":{"id":10,"petId":1,"itemCode":"FEED","itemName":"급여","intervalDays":3,"isEnabled":true,"cycleSource":"GUIDELINE","startDate":"2026-11-02","nextDueAt":"2026-11-05T12:00:00+09:00","versionLabel":"1.2","defaultCycleDays":3,"minDays":2,"maxDays":4,"isOutOfRange":false,"reason":null,"reasonNote":null,"modifiedAt":null,"foodName":"귀뚜라미"},"previous":{"intervalDays":5,"isEnabled":true,"startDate":"2026-11-05","cycleSource":"USER_CUSTOM","versionLabel":"1.2"}}},"error":null,"meta":{"timestamp":"2026-11-02T03:00:00","path":"/api/v1/routines/10/reset"}}
            """;
    public static final String COMPLETE_REQUEST = """
            {"clientRequestId":"c67c4e3d-fd4e-49bd-83f5-3c7d7b64c981","occurredAt":"2026-11-02T12:00:00+09:00","timePrecision":"EXACT","outcome":"EATING_TRACE_FOUND","valueNum":null,"detail":null,"memo":"인공 사료에서 먹은 흔적을 확인했어요."}
            """;
    public static final String COMPLETE_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"record":{"id":902,"recordType":"DONE","itemCode":"FEED","occurredAt":"2026-11-02T12:00:00+09:00","timePrecision":"EXACT","outcome":"EATING_TRACE_FOUND","valueNum":null,"unit":null,"reasonCode":null,"memo":"인공 사료에서 먹은 흔적을 확인했어요."},"routine":{"id":10,"lastDoneOn":"2026-11-02","nextDueOn":"2026-11-04"},"todayStatus":"DONE"}},"error":null,"meta":{"timestamp":"2026-11-02T03:00:00","path":"/api/v1/routines/10/complete"}}
            """;
    public static final String COMPLETE_OUTCOME_NOT_ALLOWED = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"OUTCOME_NOT_ALLOWED\",\"message\":\"해당 종 및 급여 유형에서 허용되지 않는 급여 결과입니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T03:00:00\",\"path\":\"/api/v1/routines/10/complete\"}}";
    public static final String CLIENT_REQUEST_ID_REUSED = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"CLIENT_REQUEST_ID_REUSED\",\"message\":\"동일한 clientRequestId가 다른 요청에 사용되었습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T03:00:00\",\"path\":\"/api/v1/routines/10/complete\"}}";
    public static final String ROUTINE_NOT_ACTIVE = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"ROUTINE_NOT_ACTIVE\",\"message\":\"비활성 루틴은 처리할 수 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T03:00:00\",\"path\":\"/api/v1/routines/10/complete\"}}";
    public static final String ROUTINE_ACTION_DUPLICATE = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"ROUTINE_ACTION_DUPLICATE\",\"message\":\"같은 날짜에 이미 완료 또는 건너뛰기 기록이 있습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T03:00:00\",\"path\":\"/api/v1/routines/10/complete\"}}";
    public static final String AUTH_REQUIRED = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"AUTH_REQUIRED\",\"message\":\"인증이 필요합니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String TOKEN_EXPIRED = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"TOKEN_EXPIRED\",\"message\":\"Access Token이 만료되었습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String TOKEN_INVALID = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"TOKEN_INVALID\",\"message\":\"Access Token이 유효하지 않습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String USER_WITHDRAWN = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"USER_WITHDRAWN\",\"message\":\"탈퇴한 사용자입니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String VALIDATION_FAILED = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청값이 누락되었거나 올바르지 않습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String ROUTINE_NOT_FOUND = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"ROUTINE_NOT_FOUND\",\"message\":\"관리 루틴을 찾을 수 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String PET_NOT_FOUND = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"PET_NOT_FOUND\",\"message\":\"개체를 찾을 수 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/today\"}}";
    public static final String PET_NOT_ACTIVE = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"PET_NOT_ACTIVE\",\"message\":\"활성 상태의 개체만 변경할 수 있습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String ROUTINE_START_DATE_LOCKED = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"ROUTINE_START_DATE_LOCKED\",\"message\":\"완료 또는 건너뛰기 기록이 있는 루틴의 시작일은 변경할 수 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10\"}}";
    public static final String APPLICABLE_GUIDELINE_NOT_FOUND = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"APPLICABLE_GUIDELINE_NOT_FOUND\",\"message\":\"현재 적용 가능한 게시 가이드라인이 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10/reset\"}}";
    public static final String CARE_STANDARD_NOT_FOUND = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"CARE_STANDARD_NOT_FOUND\",\"message\":\"현재 기준표에서 해당 관리 항목을 찾을 수 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10/reset\"}}";
    public static final String GUIDELINE_VERSION_CONFLICT = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"GUIDELINE_VERSION_CONFLICT\",\"message\":\"동일 적용일의 게시 가이드라인이 여러 개입니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-02T00:00:00\",\"path\":\"/api/v1/routines/10/reset\"}}";
}
