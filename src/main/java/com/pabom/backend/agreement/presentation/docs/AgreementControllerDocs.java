package com.pabom.backend.agreement.presentation.docs;

import com.pabom.backend.agreement.presentation.request.AgreementCompletionRequest;
import com.pabom.backend.agreement.presentation.response.AgreementCompletionResponse;
import com.pabom.backend.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Agreement", description = "회원가입 약관 동의 API")
public interface AgreementControllerDocs {

    @Operation(
            summary = "회원가입 약관 동의 완료",
            description = "Signup Token으로 필수 약관 동의를 저장하고 Access Token을 발급합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 완료 및 Access Token 발급 성공",
                    content = @Content(schema = @Schema(implementation = AgreementCompletionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "VALIDATION_FAILED: 요청값 검증 실패",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "INVALID_TOKEN: Signup Token 누락, 만료 또는 위조",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "AGREEMENT_VERSION_MISMATCH: 약관 버전 불일치",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "REQUIRED_AGREEMENT_MISSING: 필수 약관 미동의",
                    content = @Content
            )
    })
    ResponseEntity<AgreementCompletionResponse> complete(
            @Parameter(hidden = true)
            String authorization,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            name = "전체 약관 동의 요청",
                            value = """
                                    {
                                      "agreements": [
                                        {"type":"TERMS","version":"1.0","agreed":true},
                                        {"type":"PRIVACY","version":"1.0","agreed":true},
                                        {"type":"AGE_14","version":"1.0","agreed":true},
                                        {"type":"MARKETING","version":"1.0","agreed":false}
                                      ]
                                    }
                                    """
                    ))
            )
            AgreementCompletionRequest request
    );
}
