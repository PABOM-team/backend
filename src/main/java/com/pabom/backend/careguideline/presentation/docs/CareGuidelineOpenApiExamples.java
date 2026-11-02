package com.pabom.backend.careguideline.presentation.docs;

public final class CareGuidelineOpenApiExamples {
    private CareGuidelineOpenApiExamples() {}

    public static final String DRY_RUN_SUCCESS = """
        {"resultType":"SUCCESS","success":{"data":{"versionLabel":"1.2","mode":"DRY_RUN","valid":true,"persisted":false,"status":"DRAFT","counts":{"stages":3,"items":10,"standards":46,"stageThresholds":5,"descriptions":14}}},"error":null,"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String COMMIT_SUCCESS = """
        {"resultType":"SUCCESS","success":{"data":{"versionLabel":"1.2","mode":"COMMIT","valid":true,"persisted":true,"status":"PUBLISHED","counts":{"stages":3,"items":10,"standards":46,"stageThresholds":5,"descriptions":14}}},"error":null,"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String VALIDATION_FAILED = """
        {"resultType":"FAIL","success":null,"error":{"code":"GUIDELINE_IMPORT_INVALID","message":"가이드라인 CSV 검증에 실패했습니다.","details":{"counts":{"stages":3,"items":10,"standards":45,"stageThresholds":5,"descriptions":14},"errors":[{"file":"standards.csv","row":3,"column":"species_code","code":"REFERENCE_NOT_FOUND","message":"참조 코드를 찾을 수 없습니다: UNKNOWN_SPECIES"}]}},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String AUTH_REQUIRED = """
        {"resultType":"FAIL","success":null,"error":{"code":"AUTH_REQUIRED","message":"인증이 필요합니다.","details":null},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String TOKEN_EXPIRED = """
        {"resultType":"FAIL","success":null,"error":{"code":"TOKEN_EXPIRED","message":"Access Token이 만료되었습니다.","details":null},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String TOKEN_INVALID = """
        {"resultType":"FAIL","success":null,"error":{"code":"TOKEN_INVALID","message":"Access Token이 유효하지 않습니다.","details":null},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String USER_WITHDRAWN = """
        {"resultType":"FAIL","success":null,"error":{"code":"USER_WITHDRAWN","message":"탈퇴한 사용자입니다.","details":null},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String ADMIN_REQUIRED = """
        {"resultType":"FAIL","success":null,"error":{"code":"ADMIN_REQUIRED","message":"관리자 권한이 필요합니다.","details":null},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
    public static final String VERSION_DUPLICATED = """
        {"resultType":"FAIL","success":null,"error":{"code":"GUIDELINE_VERSION_DUPLICATED","message":"이미 등록된 가이드라인 버전입니다.","details":null},"meta":{"timestamp":"2026-10-31T06:00:00","path":"/api/v1/admin/care-standards/import"}}
        """;
}
