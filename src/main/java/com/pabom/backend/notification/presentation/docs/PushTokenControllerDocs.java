package com.pabom.backend.notification.presentation.docs;

import com.pabom.backend.notification.presentation.request.RegisterPushTokenRequest;
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
public interface PushTokenControllerDocs {

    @Operation(
            summary = "푸시 토큰 등록",
            description = "앱이 받은 Expo 푸시 토큰을 등록합니다. 멱등이며, 같은 토큰이 이미 있으면 "
                    + "요청한 회원으로 소유자를 옮기고 platform·appVariant·lastSeenAt을 갱신합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegisterPushTokenRequest.class),
                            examples = @ExampleObject(name = "iOS 운영 앱", value = NotificationOpenApiExamples.REGISTER_REQUEST))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "등록 성공. 응답 본문 없음", content = @Content),
            @ApiResponse(responseCode = "400", description = "요청값 누락·Enum 오류 또는 Expo 토큰 형식 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "VALIDATION_FAILED", value = NotificationOpenApiExamples.VALIDATION_FAILED),
                                    @ExampleObject(name = "INVALID_PUSH_TOKEN", value = NotificationOpenApiExamples.INVALID_PUSH_TOKEN)
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
    ResponseEntity<Void> register(
            @Parameter(hidden = true) AuthUser authUser,
            RegisterPushTokenRequest body,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "푸시 토큰 해제",
            description = "내 토큰을 삭제합니다. 앱은 로그아웃 직전에 호출합니다. 토큰은 URL 인코딩해서 보냅니다. "
                    + "없는 토큰이거나 다른 회원의 토큰이면 아무것도 지우지 않고 204를 반환합니다(멱등).",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "해제 성공 또는 이미 없는 토큰. 응답 본문 없음", content = @Content),
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
    ResponseEntity<Void> unregister(
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(description = "Expo 푸시 토큰 (URL 인코딩)", example = "ExponentPushToken%5Bxxxxxxxxxxxxxxxxxxxxxx%5D")
            String token,
            @Parameter(hidden = true) HttpServletRequest request
    );
}
