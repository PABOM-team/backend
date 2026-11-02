package com.pabom.backend.species.presentation;

import com.pabom.backend.config.OpenApiSchemas;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.species.application.service.SpeciesQueryService;
import com.pabom.backend.careguideline.application.service.SpeciesCareStandardsQueryService;
import com.pabom.backend.careguideline.application.result.SpeciesCareStandardsResult;
import com.pabom.backend.species.presentation.response.MorphResponse;
import com.pabom.backend.species.presentation.response.SpeciesResponse;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/species")
@RequiredArgsConstructor
@Tag(name = "Species", description = "파충류 종 및 모프 조회 API")
public class SpeciesController {

    private final SpeciesQueryService service;
    private final SpeciesCareStandardsQueryService careStandardsService;

    @GetMapping("/{speciesId}/care-standards")
    @Operation(summary = "종별 관리 가이드라인 조회",
            description = "Asia/Seoul 기준 현재 적용 가능한 최신 PUBLISHED 버전의 JUVENILE/ADULT 기준을 조회합니다. UNKNOWN은 JUVENILE 기준으로 fallback합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가이드라인 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.SpeciesCareStandardsEnvelope.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {"resultType":"SUCCESS","success":{"data":{"guidelineVersion":"1.2","speciesId":1,"speciesCode":"LEOPARD","speciesName":"레오파드 게코","lifeStages":[{"lifeStage":"JUVENILE","appliedLifeStage":"JUVENILE","careItems":[{"careItemCode":"FEED","careItemName":"급여","defaultCycleDays":1,"minDays":1,"maxDays":2,"defaultEnabled":true,"recordType":"FEED","feedResultSet":"INSECT","defaultFoodName":"귀뚜라미","evidenceLevel":"VET_SOURCE","sourceRefs":"VCA, RSPCA","description":"먹이 관리 설명"}]}]}},"error":null,"meta":{"timestamp":"2026-11-01T00:00:00","path":"/api/v1/species/1/care-standards"}}
                                    """))),
            @ApiResponse(responseCode = "404", description = "SPECIES_NOT_FOUND 또는 APPLICABLE_GUIDELINE_NOT_FOUND",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "SPECIES_NOT_FOUND", value = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"SPECIES_NOT_FOUND\",\"message\":\"종을 찾을 수 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-01T00:00:00\",\"path\":\"/api/v1/species/999/care-standards\"}}"),
                                    @ExampleObject(name = "APPLICABLE_GUIDELINE_NOT_FOUND", value = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"APPLICABLE_GUIDELINE_NOT_FOUND\",\"message\":\"현재 적용 가능한 게시 가이드라인이 없습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-10-31T00:00:00\",\"path\":\"/api/v1/species/1/care-standards\"}}")
                            })),
            @ApiResponse(responseCode = "409", description = "GUIDELINE_VERSION_CONFLICT",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "GUIDELINE_VERSION_CONFLICT", value = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"GUIDELINE_VERSION_CONFLICT\",\"message\":\"동일 적용일의 게시 가이드라인이 여러 개입니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-01T00:00:00\",\"path\":\"/api/v1/species/1/care-standards\"}}"))),
            @ApiResponse(responseCode = "422", description = "CARE_STANDARD_DATA_MISSING",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "CARE_STANDARD_DATA_MISSING", value = "{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"code\":\"CARE_STANDARD_DATA_MISSING\",\"message\":\"가이드라인 관리 기준 데이터가 누락되었습니다.\",\"details\":null},\"meta\":{\"timestamp\":\"2026-11-01T00:00:00\",\"path\":\"/api/v1/species/1/care-standards\"}}")))
    })
    public ResponseEntity<ApiResponseBody<SpeciesCareStandardsResult>> getCareStandards(
            @Parameter(description = "조회할 종 ID", example = "1", required = true)
            @PathVariable Long speciesId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponseBody.success(careStandardsService.get(speciesId), request));
    }

    @GetMapping
    @Operation(summary = "파충류 종 목록 조회", description = "인증 없이 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "종 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.SpeciesListEnvelope.class),
                            examples = @ExampleObject(name = "종 목록", value =
                                    com.pabom.backend.species.presentation.docs.SpeciesOpenApiExamples.SPECIES_SUCCESS)))
    })
    public ResponseEntity<ApiResponseBody<List<SpeciesResponse>>> getSpecies(
            HttpServletRequest request
    ) {
        List<SpeciesResponse> response = service.getSpecies().stream()
                .map(SpeciesResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    @GetMapping("/{speciesId}/morphs")
    @Operation(summary = "종별 모프 목록 조회", description = "인증 없이 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "종별 모프 조회 성공. ID는 예시값이며 고정값이 아닙니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.MorphListEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "크레스티드 게코", value =
                                            com.pabom.backend.species.presentation.docs.SpeciesOpenApiExamples.CRESTED_MORPHS_SUCCESS),
                                    @ExampleObject(name = "레오파드 게코", value =
                                            com.pabom.backend.species.presentation.docs.SpeciesOpenApiExamples.LEOPARD_MORPHS_SUCCESS),
                                    @ExampleObject(name = "볼파이톤", value =
                                            com.pabom.backend.species.presentation.docs.SpeciesOpenApiExamples.BALL_PYTHON_MORPHS_SUCCESS)
                            })),
            @ApiResponse(responseCode = "404", description = "SPECIES_NOT_FOUND",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "종 없음", value =
                                    com.pabom.backend.species.presentation.docs.SpeciesOpenApiExamples.SPECIES_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponseBody<List<MorphResponse>>> getMorphs(
            @PathVariable Long speciesId,
            HttpServletRequest request
    ) {
        List<MorphResponse> response = service.getMorphs(speciesId).stream()
                .map(MorphResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

}
