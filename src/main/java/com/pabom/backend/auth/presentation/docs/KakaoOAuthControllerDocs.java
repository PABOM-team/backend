package com.pabom.backend.auth.presentation.docs;

import com.pabom.backend.auth.presentation.response.KakaoAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.KakaoLoginResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Kakao OAuth", description = "사용자 저장 없이 카카오 OAuth 연동을 확인하는 API")
public interface KakaoOAuthControllerDocs {

    @Operation(
            summary = "카카오 로그인 URL 발급",
            description = """
                    카카오 로그인 URL과 CSRF 방지용 state 쿠키를 발급합니다.
                    Swagger에서 실행한 뒤 응답의 authorizationUrl을 같은 브라우저에서 여세요.
                    state 쿠키는 HttpOnly로 저장되며 5분 뒤 만료됩니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "카카오 로그인 URL 발급 성공",
            content = @Content(
                    schema = @Schema(implementation = KakaoAuthorizationUrlResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "resultType": "SUCCESS",
                              "success": {
                                "data": {
                                  "authorizationUrl": "https://kauth.kakao.com/oauth/authorize?client_id=...&redirect_uri=...&state=..."
                                }
                              },
                              "error": null,
                              "meta": {
                                "timestamp": "2026-10-05T12:00:00",
                                "path": "/api/v1/auth/kakao/login-url"
                              }
                            }
                            """)
            )
    )
    ResponseEntity<ApiResponseBody<KakaoAuthorizationUrlResponse>> issueAuthorizationUrl(
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "카카오 OAuth Callback",
            description = """
                    카카오가 호출하는 로컬 Callback입니다.
                    state를 검증하고 인가 코드를 Access Token으로 교환한 뒤 카카오 회원번호, 닉네임, 이메일을 반환합니다.
                    사용자 저장과 자체 JWT 발급은 수행하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "카카오 사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = KakaoLoginResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인가 코드, state 또는 카카오 사용자 정보 오류")
    })
    ResponseEntity<ApiResponseBody<KakaoLoginResponse>> callback(
            String code,
            String state,
            String expectedState,
            HttpServletRequest request,
            HttpServletResponse response
    );
}
