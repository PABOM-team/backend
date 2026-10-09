package com.pabom.backend.agreement.presentation.docs;

import com.pabom.backend.agreement.presentation.request.AgreementCompletionRequest;
import com.pabom.backend.agreement.presentation.response.AgreementCompletionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Agreement", description = "회원가입 약관 동의 API")
public interface AgreementControllerDocs {

    @Operation(
            summary = "회원가입 약관 동의 완료",
            description = "Signup Token으로 필수 약관 동의를 저장하고 Access Token을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 완료"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 Signup Token", content = @Content),
            @ApiResponse(responseCode = "409", description = "약관 버전 불일치", content = @Content),
            @ApiResponse(responseCode = "422", description = "필수 약관 미동의", content = @Content)
    })
    ResponseEntity<AgreementCompletionResponse> complete(
            @Parameter(
                    name = "Authorization",
                    description = "Bearer {signupToken}",
                    required = true,
                    in = ParameterIn.HEADER
            )
            String authorization,
            AgreementCompletionRequest request
    );
}
