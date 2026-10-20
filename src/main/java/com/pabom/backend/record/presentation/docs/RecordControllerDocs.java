package com.pabom.backend.record.presentation.docs;

import static com.pabom.backend.record.presentation.docs.RecordOpenApiExamples.*;
import com.pabom.backend.config.*;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.record.domain.type.RecordType;
import com.pabom.backend.record.presentation.request.*;
import com.pabom.backend.record.presentation.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;

@Tag(name = "Record", description = "기록 관련 API")
public interface RecordControllerDocs {
    @Operation(summary = "기록 목록 조회", description = "occurredAt DESC, id DESC 커서 페이지네이션. includeDeleted=true인 경우 최근 30일 내 삭제 기록을 포함합니다.", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
        @ApiResponse(responseCode="200", description="조회 성공", content=@Content(schema=@Schema(implementation=OpenApiSchemas.RecordListEnvelope.class), examples=@ExampleObject(name="목록", value=LIST_SUCCESS))),
        @ApiResponse(responseCode="400", description="쿼리 검증 실패", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples=@ExampleObject(name="VALIDATION_FAILED", value=VALIDATION_FAILED))),
        @ApiResponse(responseCode="401", description="인증 오류", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED),@ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED),@ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID),@ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
        @ApiResponse(responseCode="404", description="소유 개체 없음", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples=@ExampleObject(name="PET_NOT_FOUND",value=PET_NOT_FOUND)))
    })
    ResponseEntity<ApiResponseBody<RecordListResponse>> list(
        @Parameter(description="본인 소유 개체 ID") Long petId,
        @Parameter(description="쉼표 구분 관리 항목 코드", example="FEEDING,WEIGHT") String itemCodes,
        @Parameter(description="기록 타입", schema=@Schema(allowableValues={"DONE","SKIPPED"})) RecordType recordType,
        @Parameter(description="조회 시작일(포함)") LocalDate from,
        @Parameter(description="조회 종료일(포함), 최대 366일") LocalDate to,
        @Parameter(description="최근 30일 삭제 기록 포함", example="false") boolean includeDeleted,
        @Parameter(description="Base64 URL-safe 커서") String cursor,
        @Parameter(description="1~50", example="20") int size,
        @Parameter(hidden=true) AuthUser authUser, @Parameter(hidden=true) HttpServletRequest servletRequest);

    @Operation(summary="직접 기록 추가", description="관리 일정 없이도 생성되며 recordType=DONE, source=MANUAL입니다.", security=@SecurityRequirement(name=SwaggerConfig.AUTHORIZATION), requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true, content=@Content(schema=@Schema(implementation=CreateRecordRequest.class), examples=@ExampleObject(value=CREATE_REQUEST))))
    @ApiResponses({
        @ApiResponse(responseCode="201", description="생성 성공", content=@Content(schema=@Schema(implementation=OpenApiSchemas.RecordMutationEnvelope.class), examples={@ExampleObject(name="일정 없음",value=CREATE_SUCCESS),@ExampleObject(name="일정 있음",value=CREATE_WITH_ROUTINE_SUCCESS)})),
        @ApiResponse(responseCode="400", description="요청 검증 실패", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples=@ExampleObject(name="VALIDATION_FAILED",value=VALIDATION_FAILED))),
        @ApiResponse(responseCode="401", description="인증 오류", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED),@ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED),@ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID),@ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
        @ApiResponse(responseCode="404", description="소유 개체 또는 파일 없음", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="PET_NOT_FOUND",value=PET_NOT_FOUND),@ExampleObject(name="FILE_NOT_FOUND",value=FILE_NOT_FOUND)})),
        @ApiResponse(responseCode="409", description="상태 또는 멱등키 충돌", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="PET_NOT_ACTIVE",value=PET_NOT_ACTIVE),@ExampleObject(name="CLIENT_REQUEST_ID_REUSED",value=CLIENT_REQUEST_ID_REUSED)})),
        @ApiResponse(responseCode="422", description="기록 규칙 검증 실패", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="CARE_ITEM_INVALID",value=CARE_ITEM_INVALID),@ExampleObject(name="OUTCOME_NOT_ALLOWED",value=OUTCOME_NOT_ALLOWED),@ExampleObject(name="VALUE_REQUIRED",value=VALUE_REQUIRED),@ExampleObject(name="VALUE_OUT_OF_RANGE",value=VALUE_OUT_OF_RANGE),@ExampleObject(name="MEMO_REQUIRED",value=MEMO_REQUIRED),@ExampleObject(name="OCCURRED_AT_IN_FUTURE",value=OCCURRED_AT_IN_FUTURE),@ExampleObject(name="OCCURRED_AT_TOO_OLD",value=OCCURRED_AT_TOO_OLD),@ExampleObject(name="MEMO_CONTACT_NOT_ALLOWED",value=MEMO_CONTACT_NOT_ALLOWED)}))
    })
    ResponseEntity<ApiResponseBody<RecordMutationResponse>> create(@Parameter(hidden=true) AuthUser authUser, CreateRecordRequest body, @Parameter(hidden=true) HttpServletRequest request);

    @Operation(summary="기록 상세 및 정정 이력 조회", security=@SecurityRequirement(name=SwaggerConfig.AUTHORIZATION))
    @ApiResponses({
        @ApiResponse(responseCode="200", description="조회 성공", content=@Content(schema=@Schema(implementation=OpenApiSchemas.RecordDetailEnvelope.class), examples=@ExampleObject(value=DETAIL_SUCCESS))),
        @ApiResponse(responseCode="401", description="인증 오류", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED),@ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED),@ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID),@ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
        @ApiResponse(responseCode="404", description="기록 없음 또는 타 사용자 기록", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples=@ExampleObject(name="RECORD_NOT_FOUND",value=RECORD_NOT_FOUND)))
    })
    ResponseEntity<ApiResponseBody<RecordDetailResponse>> get(@Parameter(description="기록 ID",required=true) Long recordId, @Parameter(hidden=true) AuthUser authUser, @Parameter(hidden=true) HttpServletRequest request);

    @Operation(summary="기록 정정", description="revisionNo 낙관적 잠금을 사용합니다.", security=@SecurityRequirement(name=SwaggerConfig.AUTHORIZATION), requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true, content=@Content(schema=@Schema(implementation=UpdateRecordRequest.class), examples=@ExampleObject(value=UPDATE_REQUEST))))
    @ApiResponses({
        @ApiResponse(responseCode="200", description="정정 성공", content=@Content(schema=@Schema(implementation=OpenApiSchemas.RecordMutationEnvelope.class), examples={@ExampleObject(name="일정 없음",value=UPDATE_SUCCESS),@ExampleObject(name="일정 있음",value=CREATE_WITH_ROUTINE_SUCCESS)})),
        @ApiResponse(responseCode="400", description="요청 검증 실패", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples=@ExampleObject(name="VALIDATION_FAILED",value=VALIDATION_FAILED))),
        @ApiResponse(responseCode="401", description="인증 오류", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED),@ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED),@ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID),@ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),
        @ApiResponse(responseCode="404", description="기록 또는 파일 없음", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="RECORD_NOT_FOUND",value=RECORD_NOT_FOUND),@ExampleObject(name="FILE_NOT_FOUND",value=FILE_NOT_FOUND)})),
        @ApiResponse(responseCode="409", description="수정 충돌", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="RECORD_CONFLICT",value=RECORD_CONFLICT),@ExampleObject(name="RECORD_DELETED",value=RECORD_DELETED)})),
        @ApiResponse(responseCode="422", description="정정 규칙 검증 실패", content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class), examples={@ExampleObject(name="FIELD_NOT_EDITABLE",value=FIELD_NOT_EDITABLE),@ExampleObject(name="OUTCOME_NOT_ALLOWED",value=OUTCOME_NOT_ALLOWED),@ExampleObject(name="VALUE_REQUIRED",value=VALUE_REQUIRED),@ExampleObject(name="VALUE_OUT_OF_RANGE",value=VALUE_OUT_OF_RANGE),@ExampleObject(name="OCCURRED_AT_IN_FUTURE",value=OCCURRED_AT_IN_FUTURE),@ExampleObject(name="OCCURRED_AT_TOO_OLD",value=OCCURRED_AT_TOO_OLD),@ExampleObject(name="MEMO_CONTACT_NOT_ALLOWED",value=MEMO_CONTACT_NOT_ALLOWED)}))
    })
    ResponseEntity<ApiResponseBody<RecordMutationResponse>> update(@Parameter(description="기록 ID",required=true) Long recordId, @Parameter(hidden=true) AuthUser authUser, UpdateRecordRequest body, @Parameter(hidden=true) HttpServletRequest request);

    @Operation(summary="기록 Soft Delete", security=@SecurityRequirement(name=SwaggerConfig.AUTHORIZATION))
    @ApiResponses({@ApiResponse(responseCode="200",description="삭제 성공 또는 멱등 재요청",content=@Content(schema=@Schema(implementation=OpenApiSchemas.DeleteRecordEnvelope.class),examples=@ExampleObject(value=DELETE_SUCCESS))),@ApiResponse(responseCode="401",description="인증 오류",content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class),examples={@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED),@ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED),@ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID),@ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),@ApiResponse(responseCode="404",description="기록 없음",content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class),examples=@ExampleObject(name="RECORD_NOT_FOUND",value=RECORD_NOT_FOUND)))})
    ResponseEntity<ApiResponseBody<DeleteRecordResponse>> delete(@Parameter(description="기록 ID",required=true) Long recordId,@Parameter(hidden=true) AuthUser authUser,@Parameter(hidden=true) HttpServletRequest request);

    @Operation(summary="삭제 기록 복구", security=@SecurityRequirement(name=SwaggerConfig.AUTHORIZATION))
    @ApiResponses({@ApiResponse(responseCode="200",description="복구 성공",content=@Content(schema=@Schema(implementation=OpenApiSchemas.RecordMutationEnvelope.class),examples=@ExampleObject(value=RESTORE_SUCCESS))),@ApiResponse(responseCode="401",description="인증 오류",content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class),examples={@ExampleObject(name="AUTH_REQUIRED",value=AUTH_REQUIRED),@ExampleObject(name="TOKEN_EXPIRED",value=TOKEN_EXPIRED),@ExampleObject(name="TOKEN_INVALID",value=TOKEN_INVALID),@ExampleObject(name="USER_WITHDRAWN",value=USER_WITHDRAWN)})),@ApiResponse(responseCode="404",description="기록 없음",content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class),examples=@ExampleObject(name="RECORD_NOT_FOUND",value=RECORD_NOT_FOUND))),@ApiResponse(responseCode="409",description="복구 상태 충돌",content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class),examples={@ExampleObject(name="RECORD_NOT_DELETED",value=RECORD_NOT_DELETED),@ExampleObject(name="PET_NOT_ACTIVE",value=PET_NOT_ACTIVE)})),@ApiResponse(responseCode="410",description="복구 기한 만료",content=@Content(schema=@Schema(implementation=OpenApiSchemas.ErrorEnvelope.class),examples=@ExampleObject(name="RESTORE_WINDOW_EXPIRED",value=RESTORE_WINDOW_EXPIRED)))})
    ResponseEntity<ApiResponseBody<RecordMutationResponse>> restore(@Parameter(description="기록 ID",required=true) Long recordId,@Parameter(hidden=true) AuthUser authUser,@Parameter(hidden=true) HttpServletRequest request);
}
