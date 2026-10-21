package com.pabom.backend.pet.presentation;

import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.CREATE_REQUEST;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.ADOPTED_AT_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.CREATE_SUCCESS;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.CUSTOM_MORPH_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.DETAIL_SUCCESS;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.HATCHED_AT_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.INVALID_TOKEN;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.LIFE_STAGE_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.LIST_SUCCESS;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.MORPH_COMBINATION_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.MORPH_DUPLICATED;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.MORPH_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.PET_NAME_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.PET_NOT_FOUND;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.PET_PHOTO_INVALID;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.SPECIES_NOT_FOUND;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.STAGE_REQUIRED;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.UPDATE_REQUEST;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.UPDATE_SUCCESS;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.VALIDATION_FAILED;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.DELETE_INVALID_TOKEN;
import static com.pabom.backend.pet.presentation.docs.PetOpenApiExamples.DELETE_PET_NOT_FOUND;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.config.OpenApiSchemas;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.pet.application.service.PetCommandService;
import com.pabom.backend.pet.application.service.PetQueryService;
import com.pabom.backend.pet.presentation.request.CreatePetRequest;
import com.pabom.backend.pet.presentation.request.UpdatePetRequest;
import com.pabom.backend.pet.presentation.response.PetResponse;
import com.pabom.backend.pet.presentation.response.PetListResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Tag(name = "Pet", description = "개체 관련 API")
public class PetController {

    private final PetCommandService commandService;
    private final PetQueryService queryService;

    @PostMapping
    @Operation(
            summary = "개체 등록",
            description = "인증 사용자를 소유자로 등록합니다. adoptedAt과 photoFileId를 포함한 선택 필드는 null 또는 생략할 수 있습니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePetRequest.class),
                            examples = @ExampleObject(name = "개체 등록", value = CREATE_REQUEST))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.PetEnvelope.class),
                            examples = @ExampleObject(name = "등록 성공", value = CREATE_SUCCESS))),
            @ApiResponse(responseCode = "400",
                    description = "요청값 또는 개체 기본 정보 검증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "VALIDATION_FAILED", value = VALIDATION_FAILED),
                                    @ExampleObject(name = "MORPH_INVALID", value = MORPH_INVALID),
                                    @ExampleObject(name = "MORPH_DUPLICATED", value = MORPH_DUPLICATED),
                                    @ExampleObject(name = "MORPH_COMBINATION_INVALID", value = MORPH_COMBINATION_INVALID),
                                    @ExampleObject(name = "CUSTOM_MORPH_INVALID", value = CUSTOM_MORPH_INVALID),
                                    @ExampleObject(name = "PET_NAME_INVALID", value = PET_NAME_INVALID),
                                    @ExampleObject(name = "HATCHED_AT_INVALID", value = HATCHED_AT_INVALID),
                                    @ExampleObject(name = "ADOPTED_AT_INVALID", value = ADOPTED_AT_INVALID),
                                    @ExampleObject(name = "PET_PHOTO_INVALID", value = PET_PHOTO_INVALID)
                            })),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "INVALID_TOKEN", value = INVALID_TOKEN))),
            @ApiResponse(responseCode = "404", description = "SPECIES_NOT_FOUND",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "SPECIES_NOT_FOUND", value = SPECIES_NOT_FOUND))),
            @ApiResponse(responseCode = "422", description = "성장 단계와 정확성 조합 검증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "STAGE_REQUIRED", value = STAGE_REQUIRED),
                                    @ExampleObject(name = "LIFE_STAGE_INVALID", value = LIFE_STAGE_INVALID)
                            }))
    })
    public ResponseEntity<ApiResponseBody<PetResponse>> create(
            @Valid @RequestBody CreatePetRequest body,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        PetResponse response = PetResponse.from(commandService.create(authUser.userId(), body.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseBody.success(response, request));
    }

    @GetMapping
    @Operation(summary = "내 개체 목록 조회",
            description = "인증 사용자 소유 개체를 생성일 내림차순으로 조회합니다. 현재 status 필터는 지원하지 않습니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.PetListEnvelope.class),
                            examples = @ExampleObject(name = "내 개체 목록", value = LIST_SUCCESS))),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "INVALID_TOKEN", value = INVALID_TOKEN)))
    })
    public ResponseEntity<ApiResponseBody<List<PetListResponse>>> getAll(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        List<PetListResponse> response = queryService.getAll(authUser.userId()).stream()
                .map(PetListResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    @GetMapping("/{petId}")
    @Operation(summary = "내 개체 상세 조회", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.PetEnvelope.class),
                            examples = @ExampleObject(name = "개체 상세", value = DETAIL_SUCCESS))),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "INVALID_TOKEN", value = INVALID_TOKEN))),
            @ApiResponse(responseCode = "404", description = "PET_NOT_FOUND. 타인 소유 개체도 동일하게 처리합니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "PET_NOT_FOUND", value = PET_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponseBody<PetResponse>> get(
            @PathVariable Long petId,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        return ResponseEntity.ok(ApiResponseBody.success(
                PetResponse.from(queryService.get(authUser.userId(), petId)),
                request
        ));
    }

    @PatchMapping("/{petId}")
    @Operation(
            summary = "내 개체 기본 정보 수정",
            description = "생략한 필드는 유지합니다. morphIds 빈 배열은 전체 해제이며 customMorph, hatchedAt, adoptedAt, lifeStageId, photoFileId의 명시적 null은 해당 값을 삭제합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdatePetRequest.class),
                            examples = @ExampleObject(name = "개체 수정", value = UPDATE_REQUEST))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.PetEnvelope.class),
                            examples = @ExampleObject(name = "수정 성공", value = UPDATE_SUCCESS))),
            @ApiResponse(responseCode = "400", description = "요청값 또는 수정 후 최종 상태 검증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "VALIDATION_FAILED", value = VALIDATION_FAILED),
                                    @ExampleObject(name = "MORPH_INVALID", value = MORPH_INVALID),
                                    @ExampleObject(name = "MORPH_DUPLICATED", value = MORPH_DUPLICATED),
                                    @ExampleObject(name = "MORPH_COMBINATION_INVALID", value = MORPH_COMBINATION_INVALID),
                                    @ExampleObject(name = "CUSTOM_MORPH_INVALID", value = CUSTOM_MORPH_INVALID),
                                    @ExampleObject(name = "PET_NAME_INVALID", value = PET_NAME_INVALID),
                                    @ExampleObject(name = "HATCHED_AT_INVALID", value = HATCHED_AT_INVALID),
                                    @ExampleObject(name = "ADOPTED_AT_INVALID", value = ADOPTED_AT_INVALID),
                                    @ExampleObject(name = "PET_PHOTO_INVALID", value = PET_PHOTO_INVALID)
                            })),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "INVALID_TOKEN", value = INVALID_TOKEN))),
            @ApiResponse(responseCode = "404", description = "PET_NOT_FOUND. 타인 소유 개체도 동일하게 처리합니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "PET_NOT_FOUND", value = PET_NOT_FOUND))),
            @ApiResponse(responseCode = "422", description = "성장 단계와 정확성의 최종 조합 검증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = {
                                    @ExampleObject(name = "STAGE_REQUIRED", value = STAGE_REQUIRED),
                                    @ExampleObject(name = "LIFE_STAGE_INVALID", value = LIFE_STAGE_INVALID)
                            }))
    })
    public ResponseEntity<ApiResponseBody<PetResponse>> update(
            @PathVariable Long petId,
            @Valid @RequestBody UpdatePetRequest body,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        return ResponseEntity.ok(ApiResponseBody.success(
                PetResponse.from(commandService.update(authUser.userId(), petId, body.toCommand())),
                request
        ));
    }

    @DeleteMapping("/{petId}")
    @Operation(
            summary = "개체 삭제",
            description = "소유 개체와 활성 기록을 Soft Delete합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "INVALID_TOKEN", value = DELETE_INVALID_TOKEN))),
            @ApiResponse(responseCode = "404", description = "PET_NOT_FOUND. 미존재, 타인 소유, 이미 삭제된 개체를 동일하게 처리합니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class),
                            examples = @ExampleObject(name = "PET_NOT_FOUND", value = DELETE_PET_NOT_FOUND)))
    })
    public ResponseEntity<Void> delete(
            @PathVariable Long petId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        requireAuthentication(authUser);
        commandService.delete(authUser.userId(), petId);
        return ResponseEntity.noContent().build();
    }

    private void requireAuthentication(AuthUser authUser) {
        if (authUser == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
