package com.pabom.backend.careguideline.presentation.docs;

import static com.pabom.backend.careguideline.presentation.docs.RoutineOpenApiExamples.*;

import com.pabom.backend.careguideline.application.result.RoutineMutationResult;
import com.pabom.backend.careguideline.application.result.TodayResult;
import com.pabom.backend.careguideline.application.result.RoutineActionResult;
import com.pabom.backend.careguideline.application.result.RoutinePostponeResult;
import com.pabom.backend.careguideline.presentation.request.UpdateRoutineRequest;
import com.pabom.backend.careguideline.presentation.request.CompleteRoutineRequest;
import com.pabom.backend.careguideline.presentation.request.SkipRoutineRequest;
import com.pabom.backend.careguideline.presentation.request.PostponeRoutineRequest;
import com.pabom.backend.config.OpenApiSchemas;
import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;

@Tag(name = "Routine", description = "오늘 할 일 및 관리 루틴 API")
public interface RoutineControllerDocs {
    @Operation(summary = "오늘 할 일 조회", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = OpenApiSchemas.TodayEnvelope.class), examples = @ExampleObject(value = TODAY_SUCCESS))),
            @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = {@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED), @ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED), @ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID), @ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
            @ApiResponse(responseCode = "404", description = "개체 없음", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = @ExampleObject(name="PET_NOT_FOUND",value=PET_NOT_FOUND)))
    })
    ResponseEntity<ApiResponseBody<TodayResult>> today(Long petId, LocalDate date,
            @Parameter(hidden = true) AuthUser authUser, @Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "관리 루틴 수정", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = OpenApiSchemas.RoutineMutationEnvelope.class), examples = @ExampleObject(value = UPDATE_SUCCESS))),
            @ApiResponse(responseCode = "400", description = "검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = @ExampleObject(name="VALIDATION_FAILED",value=VALIDATION_FAILED))),
            @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = {@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED), @ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED), @ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID), @ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
            @ApiResponse(responseCode = "404", description = "루틴 없음", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = @ExampleObject(name="ROUTINE_NOT_FOUND",value=ROUTINE_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "상태 충돌", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = {@ExampleObject(name="PET_NOT_ACTIVE",value=PET_NOT_ACTIVE), @ExampleObject(name="ROUTINE_START_DATE_LOCKED",value=ROUTINE_START_DATE_LOCKED)}))
    })
    ResponseEntity<ApiResponseBody<RoutineMutationResult>> update(Long routineId, AuthUser authUser,
            UpdateRoutineRequest body, HttpServletRequest request);

    @Operation(summary = "관리 루틴 기준표 초기화", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초기화 성공", content = @Content(schema = @Schema(implementation = OpenApiSchemas.RoutineMutationEnvelope.class), examples = @ExampleObject(value = RESET_SUCCESS))),
            @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = {@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED), @ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED), @ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID), @ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
            @ApiResponse(responseCode = "404", description = "루틴 또는 기준표 없음", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = {@ExampleObject(name="ROUTINE_NOT_FOUND",value=ROUTINE_NOT_FOUND), @ExampleObject(name="APPLICABLE_GUIDELINE_NOT_FOUND",value=APPLICABLE_GUIDELINE_NOT_FOUND), @ExampleObject(name="CARE_STANDARD_NOT_FOUND",value=CARE_STANDARD_NOT_FOUND)})),
            @ApiResponse(responseCode = "409", description = "상태 충돌", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = {@ExampleObject(name="PET_NOT_ACTIVE",value=PET_NOT_ACTIVE), @ExampleObject(name="GUIDELINE_VERSION_CONFLICT",value=GUIDELINE_VERSION_CONFLICT)}))
    })
    ResponseEntity<ApiResponseBody<RoutineMutationResult>> reset(Long routineId, AuthUser authUser,
            HttpServletRequest request);

    @Operation(summary = "루틴 완료", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료 성공", content = @Content(schema = @Schema(implementation = OpenApiSchemas.RoutineActionEnvelope.class))),
            @ApiResponse(responseCode = "400", description = "요청 검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = @ExampleObject(value = VALIDATION_FAILED))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = @ExampleObject(value = AUTH_REQUIRED))),
            @ApiResponse(responseCode = "404", description = "루틴 없음", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class), examples = @ExampleObject(value = ROUTINE_NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "중복 또는 상태 충돌", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "422", description = "결과 또는 날짜 검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class)))
    })
    ResponseEntity<ApiResponseBody<RoutineActionResult>> complete(Long routineId, AuthUser authUser,
            CompleteRoutineRequest body, HttpServletRequest request);

    @Operation(summary = "루틴 건너뛰기", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건너뛰기 성공", content = @Content(schema = @Schema(implementation = OpenApiSchemas.RoutineActionEnvelope.class))),
            @ApiResponse(responseCode = "400", description = "요청 검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "404", description = "루틴 없음", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "409", description = "중복 또는 상태 충돌", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "422", description = "날짜 검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class)))
    })
    ResponseEntity<ApiResponseBody<RoutineActionResult>> skip(Long routineId, AuthUser authUser,
            SkipRoutineRequest body, HttpServletRequest request);

    @Operation(summary = "루틴 미루기 또는 취소", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "미루기 변경 성공", content = @Content(schema = @Schema(implementation = OpenApiSchemas.RoutinePostponeEnvelope.class))),
            @ApiResponse(responseCode = "400", description = "요청 검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "404", description = "루틴 없음", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "409", description = "취소 또는 상태 충돌", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class))),
            @ApiResponse(responseCode = "422", description = "미루기 날짜 검증 실패", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorEnvelope.class)))
    })
    ResponseEntity<ApiResponseBody<RoutinePostponeResult>> postpone(Long routineId, AuthUser authUser,
            PostponeRoutineRequest body, HttpServletRequest request);
}
