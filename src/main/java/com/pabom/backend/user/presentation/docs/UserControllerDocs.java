package com.pabom.backend.user.presentation.docs;

import com.pabom.backend.config.OpenApiSchemas;
import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.user.presentation.request.WithdrawalRequest;
import com.pabom.backend.user.presentation.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "현재 사용자 정보 조회 및 회원 탈퇴 API")
public interface UserControllerDocs {

    @Operation(
            summary = "내 정보 조회",
            description = "카카오·구글 로그인 사용자의 기본 정보, 약관 동의 상태와 개체 수를 조회합니다. PENDING_TERMS 사용자도 조회할 수 있습니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.UserProfileEnvelope.class),
                            examples = @ExampleObject(name = "조회 성공", value = PROFILE_SUCCESS))),
            @ApiResponse(responseCode = "401",
                    description = "인증 정보가 없거나 Access Token이 만료·위조되었거나 탈퇴한 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH_REQUIRED", value = AUTH_REQUIRED),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = TOKEN_EXPIRED),
                                    @ExampleObject(name = "TOKEN_INVALID", value = TOKEN_INVALID),
                                    @ExampleObject(name = "USER_WITHDRAWN", value = USER_WITHDRAWN)
                            }))
    })
    ResponseEntity<ApiResponseBody<UserProfileResponse>> getMe(
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "회원 탈퇴",
            description = "PABOM 회원 계정을 탈퇴 처리하고 모든 Refresh Token을 폐기합니다. 외부 소셜 계정의 연결은 해제하지 않습니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WithdrawalRequest.class),
                            examples = @ExampleObject(name = "탈퇴 확인", value = WITHDRAW_REQUEST))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "회원 탈퇴 성공. 응답 본문은 없으며 pabom_rt 쿠키가 즉시 만료됩니다.",
                    headers = @Header(name = "Set-Cookie",
                            description = "pabom_rt=; Path=/api/v1/auth; Max-Age=0; HttpOnly",
                            schema = @Schema(type = "string")),
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "인증 정보가 없거나 Access Token이 만료·위조되었거나 이미 탈퇴한 사용자",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "AUTH_REQUIRED", value = AUTH_REQUIRED),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = TOKEN_EXPIRED),
                                    @ExampleObject(name = "TOKEN_INVALID", value = TOKEN_INVALID),
                                    @ExampleObject(name = "USER_WITHDRAWN", value = USER_WITHDRAWN)
                            })),
            @ApiResponse(responseCode = "422", description = "confirm이 누락되었거나 true가 아님",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "CONFIRM_REQUIRED", value = CONFIRM_REQUIRED)))
    })
    ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) AuthUser authUser,
            WithdrawalRequest withdrawalRequest,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    );

    String PROFILE_SUCCESS = """
            {
              "resultType":"SUCCESS",
              "success":{"data":{"id":101,"nickname":"테스트","status":"ACTIVE","role":"USER","region":null,"agreements":[{"type":"TERMS","version":"1.0","agreed":true,"agreedAt":"2026-10-17T03:00:00Z"}],"requiredAgreements":[{"type":"TERMS","currentVersion":"1.0","required":true}],"needsReconsent":false,"petCount":{"active":0,"ended":0},"createdAt":"2026-10-17T02:00:00Z"}},
              "error":null,
              "meta":{"timestamp":"2026-10-19T05:00:00","path":"/api/v1/users/me"}
            }
            """;
    String WITHDRAW_REQUEST = "{\"confirm\":true}";
    String AUTH_REQUIRED = """
            {"resultType":"FAIL","success":null,"error":{"code":"AUTH_REQUIRED","message":"인증이 필요합니다.","details":null},"meta":{"timestamp":"2026-10-19T05:00:00","path":"/api/v1/users/me"}}
            """;
    String TOKEN_EXPIRED = """
            {"resultType":"FAIL","success":null,"error":{"code":"TOKEN_EXPIRED","message":"Access Token이 만료되었습니다.","details":null},"meta":{"timestamp":"2026-10-19T05:00:00","path":"/api/v1/users/me"}}
            """;
    String TOKEN_INVALID = """
            {"resultType":"FAIL","success":null,"error":{"code":"TOKEN_INVALID","message":"Access Token이 유효하지 않습니다.","details":null},"meta":{"timestamp":"2026-10-19T05:00:00","path":"/api/v1/users/me"}}
            """;
    String USER_WITHDRAWN = """
            {"resultType":"FAIL","success":null,"error":{"code":"USER_WITHDRAWN","message":"탈퇴한 사용자입니다.","details":null},"meta":{"timestamp":"2026-10-19T05:00:00","path":"/api/v1/users/me"}}
            """;
    String CONFIRM_REQUIRED = """
            {"resultType":"FAIL","success":null,"error":{"code":"CONFIRM_REQUIRED","message":"회원 탈퇴 확인이 필요합니다.","details":null},"meta":{"timestamp":"2026-10-19T05:00:00","path":"/api/v1/users/me"}}
            """;
}
