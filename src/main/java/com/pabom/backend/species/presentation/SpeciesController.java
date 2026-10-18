package com.pabom.backend.species.presentation;

import com.pabom.backend.config.OpenApiSchemas;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.species.application.service.SpeciesQueryService;
import com.pabom.backend.species.presentation.response.MorphResponse;
import com.pabom.backend.species.presentation.response.SpeciesResponse;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
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
