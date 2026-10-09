package com.pabom.backend.auth.presentation.docs;

import com.pabom.backend.auth.presentation.response.TokenRefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth Session", description = "인증 세션 갱신 및 로그아웃 API")
public interface AuthSessionControllerDocs {

    @Operation(summary = "Access Token 재발급")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 재사용된 Refresh Token"),
            @ApiResponse(responseCode = "403", description = "탈퇴 사용자 또는 허용되지 않은 Origin")
    })
    ResponseEntity<TokenRefreshResponse> refresh(
            @Parameter(hidden = true) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    );

    @Operation(summary = "로그아웃")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 처리 완료"),
            @ApiResponse(responseCode = "403", description = "허용되지 않은 Origin")
    })
    ResponseEntity<Void> logout(
            @Parameter(hidden = true) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    );
}
