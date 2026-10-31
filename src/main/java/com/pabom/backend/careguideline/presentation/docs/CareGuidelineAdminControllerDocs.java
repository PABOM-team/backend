package com.pabom.backend.careguideline.presentation.docs;

import static com.pabom.backend.careguideline.presentation.docs.CareGuidelineOpenApiExamples.*;
import com.pabom.backend.careguideline.domain.type.CareGuidelineImportMode;
import com.pabom.backend.careguideline.presentation.response.CareGuidelineImportResponse;
import com.pabom.backend.config.*;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Care Guideline", description = "관리자용 관리 가이드라인 API")
public interface CareGuidelineAdminControllerDocs {
    @Operation(
            summary = "API-92 관리 가이드라인 CSV Import",
            description = "UTF-8 CSV 5개를 함께 검증합니다. DRY_RUN은 저장하지 않고, COMMIT은 전체 검증 성공 시 한 트랜잭션으로 저장합니다. publish=true이면 effectiveFrom이 필수입니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 또는 등록 성공",
                    content = @Content(schema = @Schema(implementation = OpenApiSchemas.CareGuidelineImportEnvelope.class),
                            examples = {@ExampleObject(name = "DRY_RUN 성공", value = DRY_RUN_SUCCESS),
                                    @ExampleObject(name = "COMMIT 및 게시 성공", value = COMMIT_SUCCESS)})),
            @ApiResponse(responseCode = "400", description = "CSV 헤더·필수값·타입·중복·참조·주기 검증 실패",
                    content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "행 단위 검증 실패", value = VALIDATION_FAILED))),
            @ApiResponse(responseCode = "401", description = "인증 필요 또는 토큰 오류",
                    content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {@ExampleObject(name = "AUTH_REQUIRED", value = AUTH_REQUIRED),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = TOKEN_EXPIRED),
                                    @ExampleObject(name = "TOKEN_INVALID", value = TOKEN_INVALID),
                                    @ExampleObject(name = "USER_WITHDRAWN", value = USER_WITHDRAWN)})),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "ADMIN_REQUIRED", value = ADMIN_REQUIRED))),
            @ApiResponse(responseCode = "409", description = "동일 versionLabel 등록됨",
                    content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "GUIDELINE_VERSION_DUPLICATED", value = VERSION_DUPLICATED)))
    })
    ResponseEntity<ApiResponseBody<CareGuidelineImportResponse>> importCsv(
            @Parameter(description = "가이드라인 버전. CSV guideline_version과 정확히 일치", example = "1.2", required = true) String versionLabel,
            @Parameter(description = "DRY_RUN 또는 COMMIT", example = "DRY_RUN", required = true) CareGuidelineImportMode mode,
            @Parameter(description = "COMMIT 시 즉시 게시 여부", example = "false") boolean publish,
            @Parameter(description = "게시 적용일. publish=true이면 필수", example = "2026-11-01") LocalDate effectiveFrom,
            @Parameter(description = "stages.csv", required = true, schema = @Schema(type = "string", format = "binary")) MultipartFile stages,
            @Parameter(description = "items.csv", required = true, schema = @Schema(type = "string", format = "binary")) MultipartFile items,
            @Parameter(description = "standards.csv", required = true, schema = @Schema(type = "string", format = "binary")) MultipartFile standards,
            @Parameter(description = "stage_thresholds.csv", required = true, schema = @Schema(type = "string", format = "binary")) MultipartFile stageThresholds,
            @Parameter(description = "descriptions.csv", required = true, schema = @Schema(type = "string", format = "binary")) MultipartFile descriptions,
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(hidden = true) HttpServletRequest request);
}
