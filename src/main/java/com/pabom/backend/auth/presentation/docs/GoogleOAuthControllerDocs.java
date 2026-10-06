package com.pabom.backend.auth.presentation.docs;

import com.pabom.backend.auth.presentation.response.GoogleAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.GoogleLoginResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Google OAuth", description = "사용자 저장 없이 구글 OAuth 연동을 확인하는 API")
public interface GoogleOAuthControllerDocs {

    @Operation(
            summary = "구글 로그인 URL 발급",
            description = """
                    구글 로그인 URL과 CSRF 방지용 state 쿠키를 발급합니다.
                    Swagger에서 실행한 뒤 응답의 authorizationUrl을 같은 브라우저에서 여세요.
                    state 쿠키는 HttpOnly로 저장되며 5분 뒤 만료됩니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "구글 로그인 URL 발급 성공",
            content = @Content(
                    schema = @Schema(implementation = GoogleAuthorizationUrlResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "resultType": "SUCCESS",
                              "success": {
                                "data": {
                                  "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&state=..."
                                }
                              },
                              "error": null,
                              "meta": {
                                "timestamp": "2026-10-06T12:00:00",
                                "path": "/api/v1/auth/google/login-url"
                              }
                            }
                            """)
            )
    )
    ResponseEntity<ApiResponseBody<GoogleAuthorizationUrlResponse>> issueAuthorizationUrl(
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "구글 OAuth Callback",
            description = """
                    구글이 호출하는 로컬 Callback입니다.
                    state를 검증하고 인가 코드를 Access Token으로 교환한 뒤 구글 회원 식별자, 이름, 이메일을 반환합니다.
                    사용자 저장과 자체 JWT 발급은 수행하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "구글 사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = GoogleLoginResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인가 코드, state 또는 구글 사용자 정보 오류")
    })
    ResponseEntity<ApiResponseBody<GoogleLoginResponse>> callback(
            String code,
            String state,
            String expectedState,
            HttpServletRequest request,
            HttpServletResponse response
    );
}
