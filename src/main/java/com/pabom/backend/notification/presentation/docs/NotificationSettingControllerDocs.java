package com.pabom.backend.notification.presentation.docs;

import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.notification.presentation.request.UpdateNotificationSettingRequest;
import com.pabom.backend.notification.presentation.response.NotificationSettingResponse;
import com.pabom.backend.config.OpenApiSchemas;
import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Notification", description = "앱 푸시 토큰·알림 설정 API")
public interface NotificationSettingControllerDocs {

    @Operation(
            summary = "알림 설정 조회",
            description = "케어 알림 설정을 조회합니다. 설정한 적 없는 회원은 기본값(켜짐, 09:00, Asia/Seoul)으로 응답합니다. "
                    + "OS 알림 권한과는 별개로, 서버가 보낼지 여부만 정합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.NotificationSettingEnvelope.class),
                            examples = @ExampleObject(name = "기본값", value = NotificationOpenApiExamples.SETTINGS_SUCCESS))),
            @ApiResponse(responseCode = "401",
                    description = "인증 정보가 없거나 Access Token이 만료·위조되었거나 탈퇴한 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH_REQUIRED", value = NotificationOpenApiExamples.AUTH_REQUIRED),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = NotificationOpenApiExamples.TOKEN_EXPIRED),
                                    @ExampleObject(name = "TOKEN_INVALID", value = NotificationOpenApiExamples.TOKEN_INVALID),
                                    @ExampleObject(name = "USER_WITHDRAWN", value = NotificationOpenApiExamples.USER_WITHDRAWN)
                            }))
    })
    ResponseEntity<ApiResponseBody<NotificationSettingResponse>> get(
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "알림 설정 변경",
            description = "바꿀 필드만 보냅니다. 빠졌거나 null인 필드는 그대로 둡니다. careReminderTime은 HH:mm이며 "
                    + "분은 00 또는 30만 허용합니다(발송 주기 30분). timeZone은 IANA 이름만 허용합니다. 응답은 바뀐 전체 설정입니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateNotificationSettingRequest.class),
                            examples = @ExampleObject(name = "알림 시각만 변경", value = NotificationOpenApiExamples.SETTINGS_UPDATE_REQUEST))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공. 바뀐 전체 설정",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.NotificationSettingEnvelope.class),
                            examples = @ExampleObject(name = "변경 성공", value = NotificationOpenApiExamples.SETTINGS_UPDATE_SUCCESS))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: 알림 시각 형식·단위 또는 시간대 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "VALIDATION_FAILED (careReminderTime)", value = NotificationOpenApiExamples.INVALID_REMINDER_TIME),
                                    @ExampleObject(name = "VALIDATION_FAILED (timeZone)", value = NotificationOpenApiExamples.INVALID_TIME_ZONE)
                            })),
            @ApiResponse(responseCode = "401",
                    description = "인증 정보가 없거나 Access Token이 만료·위조되었거나 탈퇴한 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH_REQUIRED", value = NotificationOpenApiExamples.AUTH_REQUIRED),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = NotificationOpenApiExamples.TOKEN_EXPIRED),
                                    @ExampleObject(name = "TOKEN_INVALID", value = NotificationOpenApiExamples.TOKEN_INVALID),
                                    @ExampleObject(name = "USER_WITHDRAWN", value = NotificationOpenApiExamples.USER_WITHDRAWN)
                            }))
    })
    ResponseEntity<ApiResponseBody<NotificationSettingResponse>> update(
            @Parameter(hidden = true) AuthUser authUser,
            UpdateNotificationSettingRequest body,
            @Parameter(hidden = true) HttpServletRequest request
    );
}
