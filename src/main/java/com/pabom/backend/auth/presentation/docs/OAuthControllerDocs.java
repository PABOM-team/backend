package com.pabom.backend.auth.presentation.docs;

import com.pabom.backend.auth.presentation.response.OAuthAuthorizationUrlResponse;
import com.pabom.backend.auth.presentation.response.OAuthLoginResponse;
import com.pabom.backend.global.response.ApiResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "OAuth", description = "Google과 Kakao OAuth 연동을 확인하는 API")
public interface OAuthControllerDocs {

    @Operation(summary = "OAuth 로그인 URL 발급", description = "provider에 해당하는 로그인 URL과 CSRF 방지용 state 쿠키를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 URL 발급 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 OAuth 제공자", content = @Content)
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
                    description = "요청값, Redirect URI 또는 Provider 인가 코드 오류",
                    content = @Content
            ),
            @ApiResponse(responseCode = "429", description = "로그인 요청 제한 초과", content = @Content),
            @ApiResponse(responseCode = "502", description = "OAuth Provider 통신 장애", content = @Content)
    })
    ResponseEntity<OAuthLoginResponse> callback(
            @Parameter(description = "OAuth 제공자", example = "google", required = true) String provider,
            String code,
            String state,
            HttpServletRequest request,
            HttpServletResponse response
    );
}
