package com.pabom.backend.auth.presentation.docs;

import com.pabom.backend.auth.presentation.response.OAuthAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.OAuthLoginResponse;
import com.pabom.backend.auth.presentation.response.TokenRefreshResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "OAuth 로그인 및 인증 세션 관리 API")
public interface AuthControllerDocs {

    @Operation(summary = "OAuth 로그인 URL 발급", description = "provider에 해당하는 로그인 URL과 CSRF 방지용 state 쿠키를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 URL 발급 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "UNSUPPORTED_OAUTH_PROVIDER: 지원하지 않는 OAuth 제공자",
                    content = @Content
            )
    })
    ResponseEntity<ApiResponseBody<OAuthAuthorizationUrlResponse>> issueAuthorizationUrl(
            @Parameter(description = "OAuth 제공자", example = "google", required = true) String provider,
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "OAuth Callback",
            description = "OAuth 인증 후 PENDING_TERMS에는 Signup Token을, ACTIVE에는 Access/Refresh Token을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OAuth 로그인 성공",
                    content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "VALIDATION_FAILED, KAKAO_CODE_INVALID, GOOGLE_CODE_INVALID",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "INVALID_SOCIAL_TOKEN: 유효하지 않은 소셜 인증 정보",
                    content = @Content
            ),
            @ApiResponse(responseCode = "429", description = "로그인 요청 제한 초과", content = @Content),
            @ApiResponse(
                    responseCode = "502",
                    description = "KAKAO_UNAVAILABLE, GOOGLE_UNAVAILABLE: OAuth Provider 통신 장애",
                    content = @Content
            )
    })
    ResponseEntity<OAuthLoginResponse> callback(
            @Parameter(description = "OAuth 제공자", example = "google", required = true) String provider,
            String code,
            String state,
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "Access Token 재발급",
            description = """
                    HttpOnly 쿠키 pabom_rt를 검증하여 Access Token을 재발급하고,
                    Refresh Token을 Rotation하여 쿠키 만료 시간을 다시 14일로 갱신합니다.
                    Authorization Header와 Request Body는 사용하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Access Token 재발급 및 Refresh Token Rotation 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "재발급 성공", value = REFRESH_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "REFRESH_INVALID 또는 REFRESH_REUSED",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "유효하지 않은 Refresh Token",
                                            value = REFRESH_INVALID_EXAMPLE
                                    ),
                                    @ExampleObject(
                                            name = "Refresh Token 재사용 감지",
                                            value = REFRESH_REUSED_EXAMPLE
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "USER_WITHDRAWN 또는 ORIGIN_NOT_ALLOWED",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "탈퇴 사용자",
                                            value = USER_WITHDRAWN_EXAMPLE
                                    ),
                                    @ExampleObject(
                                            name = "허용되지 않은 Origin",
                                            value = ORIGIN_NOT_ALLOWED_REFRESH_EXAMPLE
                                    )
                            }
                    )
            )
    })
    ResponseEntity<TokenRefreshResponse> refresh(
            @Parameter(hidden = true) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(
            summary = "로그아웃",
            description = """
                    pabom_rt가 식별하는 Token Family를 폐기하고 쿠키를 즉시 만료시킵니다.
                    쿠키 또는 Bearer Token이 없거나 이미 폐기된 경우에도 멱등하게 성공합니다.
                    성공 여부는 응답 본문이 아니라 HTTP 204 No Content로 판단합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "로그아웃 성공. 응답 본문은 없으며 pabom_rt 쿠키가 즉시 만료됩니다.",
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "pabom_rt=; Path=/api/v1/auth; Max-Age=0; HttpOnly",
                            schema = @Schema(type = "string")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ORIGIN_NOT_ALLOWED: 허용되지 않은 Origin",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "허용되지 않은 Origin",
                                    value = ORIGIN_NOT_ALLOWED_LOGOUT_EXAMPLE
                            )
                    )
            )
    })
    ResponseEntity<Void> logout(
            @Parameter(hidden = true) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    );

    String REFRESH_SUCCESS_EXAMPLE = """
            {
              "accessToken": "eyJhbGciOi...",
              "expiresIn": 1800
            }
            """;

    String REFRESH_INVALID_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "REFRESH_INVALID",
                "message": "유효하지 않거나 만료된 리프레시 토큰입니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/auth/refresh"
              }
            }
            """;

    String REFRESH_REUSED_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "REFRESH_REUSED",
                "message": "이미 사용된 리프레시 토큰입니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/auth/refresh"
              }
            }
            """;

    String USER_WITHDRAWN_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "USER_WITHDRAWN",
                "message": "탈퇴한 사용자입니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/auth/refresh"
              }
            }
            """;

    String ORIGIN_NOT_ALLOWED_REFRESH_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "ORIGIN_NOT_ALLOWED",
                "message": "허용되지 않은 Origin입니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/auth/refresh"
              }
            }
            """;

    String ORIGIN_NOT_ALLOWED_LOGOUT_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "ORIGIN_NOT_ALLOWED",
                "message": "허용되지 않은 Origin입니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/auth/logout"
              }
            }
            """;
}
