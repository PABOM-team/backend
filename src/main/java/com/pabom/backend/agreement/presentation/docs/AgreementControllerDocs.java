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
                    content = @Content(
                            schema = @Schema(implementation = AgreementCompletionResponse.class),
                            examples = @ExampleObject(name = "회원가입 완료", value = AGREEMENT_SUCCESS_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "VALIDATION_FAILED: 요청값 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "요청값 검증 실패", value = VALIDATION_FAILED_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "INVALID_TOKEN: Signup Token 누락, 만료 또는 위조",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "유효하지 않은 Signup Token", value = INVALID_TOKEN_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "AGREEMENT_VERSION_MISMATCH: 약관 버전 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "약관 버전 불일치", value = VERSION_MISMATCH_EXAMPLE)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "REQUIRED_AGREEMENT_MISSING: 필수 약관 미동의",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "필수 약관 미동의", value = REQUIRED_AGREEMENT_MISSING_EXAMPLE)
                    )
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

    String AGREEMENT_SUCCESS_EXAMPLE = """
            {
              "user": {
                "id": 1,
                "status": "ACTIVE"
              },
              "accessToken": "eyJhbGciOi...",
              "expiresIn": 1800
            }
            """;

    String VALIDATION_FAILED_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "VALIDATION_FAILED",
                "message": "요청값이 누락되었거나 올바르지 않습니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/me/agreements"
              }
            }
            """;

    String INVALID_TOKEN_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "INVALID_TOKEN",
                "message": "유효하지 않거나 만료된 인증 토큰입니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/me/agreements"
              }
            }
            """;

    String VERSION_MISMATCH_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "AGREEMENT_VERSION_MISMATCH",
                "message": "약관 버전이 최신 버전과 일치하지 않습니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/me/agreements"
              }
            }
            """;

    String REQUIRED_AGREEMENT_MISSING_EXAMPLE = """
            {
              "resultType": "FAIL",
              "success": null,
              "error": {
                "code": "REQUIRED_AGREEMENT_MISSING",
                "message": "필수 약관에 모두 동의해야 합니다.",
                "details": null
              },
              "meta": {
                "timestamp": "2026-10-09T12:00:00",
                "path": "/api/v1/me/agreements"
              }
            }
            """;
}
