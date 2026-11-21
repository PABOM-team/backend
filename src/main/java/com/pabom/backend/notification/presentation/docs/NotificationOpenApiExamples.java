package com.pabom.backend.notification.presentation.docs;

final class NotificationOpenApiExamples {

    private NotificationOpenApiExamples() {
    }

    static final String REGISTER_REQUEST = """
            {"token":"ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]","platform":"IOS","appVariant":"PRODUCTION"}
            """;
    static final String SETTINGS_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"careReminderEnabled":true,"careReminderTime":"09:00","timeZone":"Asia/Seoul"}},"error":null,"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/users/me/notification-settings"}}
            """;
    static final String SETTINGS_UPDATE_REQUEST = """
            {"careReminderTime":"21:30"}
            """;
    static final String SETTINGS_UPDATE_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"careReminderEnabled":true,"careReminderTime":"21:30","timeZone":"Asia/Seoul"}},"error":null,"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/users/me/notification-settings"}}
            """;
    static final String PUSH_TEST_SUCCESS = """
            {"resultType":"SUCCESS","success":{"data":{"targetCount":2,"acceptedCount":2}},"error":null,"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens/test"}}
            """;

    static final String VALIDATION_FAILED = """
            {"resultType":"FAIL","success":null,"error":{"code":"VALIDATION_FAILED","message":"요청값이 누락되었거나 올바르지 않습니다.","details":{"field":"platform","message":"must not be null"}},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens"}}
            """;
    static final String INVALID_PUSH_TOKEN = """
            {"resultType":"FAIL","success":null,"error":{"code":"INVALID_PUSH_TOKEN","message":"Expo 푸시 토큰 형식이 올바르지 않습니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens"}}
            """;
    static final String INVALID_REMINDER_TIME = """
            {"resultType":"FAIL","success":null,"error":{"code":"VALIDATION_FAILED","message":"요청값이 누락되었거나 올바르지 않습니다.","details":{"field":"careReminderTime","message":"HH:mm 형식이며 분은 00 또는 30이어야 합니다."}},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/users/me/notification-settings"}}
            """;
    static final String INVALID_TIME_ZONE = """
            {"resultType":"FAIL","success":null,"error":{"code":"VALIDATION_FAILED","message":"요청값이 누락되었거나 올바르지 않습니다.","details":{"field":"timeZone","message":"IANA 시간대 이름이어야 합니다. (예: Asia/Seoul)"}},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/users/me/notification-settings"}}
            """;
    static final String PUSH_TOKEN_NOT_FOUND = """
            {"resultType":"FAIL","success":null,"error":{"code":"PUSH_TOKEN_NOT_FOUND","message":"등록된 푸시 토큰이 없습니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens/test"}}
            """;
    static final String PUSH_NOT_CONFIGURED = """
            {"resultType":"FAIL","success":null,"error":{"code":"PUSH_NOT_CONFIGURED","message":"푸시 발송 설정이 되어 있지 않습니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens/test"}}
            """;

    static final String AUTH_REQUIRED = """
            {"resultType":"FAIL","success":null,"error":{"code":"AUTH_REQUIRED","message":"인증이 필요합니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens"}}
            """;
    static final String TOKEN_EXPIRED = """
            {"resultType":"FAIL","success":null,"error":{"code":"TOKEN_EXPIRED","message":"Access Token이 만료되었습니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens"}}
            """;
    static final String TOKEN_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"TOKEN_INVALID","message":"Access Token이 유효하지 않습니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens"}}
            """;
    static final String USER_WITHDRAWN = """
            {"resultType":"FAIL","success":null,"error":{"code":"USER_WITHDRAWN","message":"탈퇴한 사용자입니다.","details":null},"meta":{"timestamp":"2026-11-21T00:00:00","path":"/api/v1/push-tokens"}}
            """;
}
