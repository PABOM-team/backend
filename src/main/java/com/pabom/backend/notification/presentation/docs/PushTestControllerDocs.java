package com.pabom.backend.notification.presentation.docs;

import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.notification.presentation.response.PushTestResponse;
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
public interface PushTestControllerDocs {

    @Operation(
            summary = "내 기기로 테스트 푸시 보내기 (local·dev 전용)",
            description = "내가 등록한 모든 푸시 토큰으로 테스트 알림을 보냅니다. 운영 서버에는 없는 API입니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 요청 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.PushTestEnvelope.class),
                            examples = @ExampleObject(name = "발송 성공", value = NotificationOpenApiExamples.PUSH_TEST_SUCCESS))),
            @ApiResponse(responseCode = "401",
                    description = "인증 정보가 없거나 Access Token이 만료·위조되었거나 탈퇴한 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH_REQUIRED", value = NotificationOpenApiExamples.AUTH_REQUIRED),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = NotificationOpenApiExamples.TOKEN_EXPIRED),
                                    @ExampleObject(name = "TOKEN_INVALID", value = NotificationOpenApiExamples.TOKEN_INVALID),
                                    @ExampleObject(name = "USER_WITHDRAWN", value = NotificationOpenApiExamples.USER_WITHDRAWN)
                            })),
            @ApiResponse(responseCode = "404", description = "PUSH_TOKEN_NOT_FOUND: 등록된 토큰 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "PUSH_TOKEN_NOT_FOUND", value = NotificationOpenApiExamples.PUSH_TOKEN_NOT_FOUND))),
            @ApiResponse(responseCode = "503", description = "PUSH_NOT_CONFIGURED: 서버에 EXPO_ACCESS_TOKEN이 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "PUSH_NOT_CONFIGURED", value = NotificationOpenApiExamples.PUSH_NOT_CONFIGURED)))
    })
    ResponseEntity<ApiResponseBody<PushTestResponse>> sendTest(
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(hidden = true) HttpServletRequest request
    );
}
