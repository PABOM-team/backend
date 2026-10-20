package com.pabom.backend.record.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.*;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.*;
import com.pabom.backend.record.application.service.RecordService;
import com.pabom.backend.record.domain.type.RecordType;
import com.pabom.backend.record.presentation.docs.RecordControllerDocs;
import com.pabom.backend.record.presentation.request.*;
import com.pabom.backend.record.presentation.response.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/records", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RecordController implements RecordControllerDocs {
    private final RecordService service;

    @Override @GetMapping
    public ResponseEntity<ApiResponseBody<RecordListResponse>> list(
            @RequestParam(required=false) Long petId, @RequestParam(required=false) String itemCodes,
            @RequestParam(required=false) RecordType recordType, @RequestParam(required=false) LocalDate from,
            @RequestParam(required=false) LocalDate to,
            @RequestParam(defaultValue="false") boolean includeDeleted,
            @RequestParam(required=false) String cursor, @RequestParam(defaultValue="20") int size,
            @AuthenticationPrincipal AuthUser authUser, HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(service.list(authUser.userId(), petId, itemCodes,
                recordType, from, to, includeDeleted, cursor, size), request));
    }

    @Override @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseBody<RecordMutationResponse>> create(
            @AuthenticationPrincipal AuthUser authUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateRecordRequest.class),
                            examples = @ExampleObject(name = "직접 기록 추가",
                                    value = com.pabom.backend.record.presentation.docs.RecordOpenApiExamples.CREATE_REQUEST)))
            @Valid @RequestBody CreateRecordRequest body,
            HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseBody.success(service.create(authUser.userId(), body), request));
    }

    @Override @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponseBody<RecordDetailResponse>> get(@PathVariable Long recordId,
            @AuthenticationPrincipal AuthUser authUser, HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(service.get(authUser.userId(), recordId), request));
    }

    @Override @PatchMapping(value = "/{recordId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseBody<RecordMutationResponse>> update(@PathVariable Long recordId,
            @AuthenticationPrincipal AuthUser authUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateRecordRequest.class),
                            examples = @ExampleObject(name = "기록 정정",
                                    value = com.pabom.backend.record.presentation.docs.RecordOpenApiExamples.UPDATE_REQUEST)))
            @Valid @RequestBody UpdateRecordRequest body,
            HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(service.update(authUser.userId(), recordId, body), request));
    }

    @Override @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponseBody<DeleteRecordResponse>> delete(@PathVariable Long recordId,
            @AuthenticationPrincipal AuthUser authUser, HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(service.delete(authUser.userId(), recordId), request));
    }

    @Override @PostMapping("/{recordId}/restore")
    public ResponseEntity<ApiResponseBody<RecordMutationResponse>> restore(@PathVariable Long recordId,
            @AuthenticationPrincipal AuthUser authUser, HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(service.restore(authUser.userId(), recordId), request));
    }

    private void requireAuthentication(AuthUser user, HttpServletRequest request) {
        if (user != null) return;
        Object code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (code instanceof BaseErrorCode errorCode) throw new BusinessException(errorCode);
        throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
    }
}
