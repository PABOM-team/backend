package com.pabom.backend.config;

import com.pabom.backend.global.response.ResultType;
import com.pabom.backend.pet.presentation.response.PetResponse;
import com.pabom.backend.pet.presentation.response.PetListResponse;
import com.pabom.backend.species.presentation.response.MorphResponse;
import com.pabom.backend.species.presentation.response.SpeciesResponse;
import com.pabom.backend.user.presentation.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** Documentation-only concrete response types for generic ApiResponseBody payloads. */
public final class OpenApiSchemas {

    private OpenApiSchemas() {
    }

    @Schema(name = "SpeciesListApiResponse")
    public record SpeciesListEnvelope(
            @Schema(example = "SUCCESS") ResultType resultType,
            SpeciesListSuccess success,
            @Schema(nullable = true) ErrorData error,
            MetaData meta
    ) {
    }

    public record SpeciesListSuccess(List<SpeciesResponse> data) {
    }

    @Schema(name = "MorphListApiResponse")
    public record MorphListEnvelope(
            @Schema(example = "SUCCESS") ResultType resultType,
            MorphListSuccess success,
            @Schema(nullable = true) ErrorData error,
            MetaData meta
    ) {
    }

    public record MorphListSuccess(List<MorphResponse> data) {
    }

    @Schema(name = "PetApiResponse")
    public record PetEnvelope(
            @Schema(example = "SUCCESS") ResultType resultType,
            PetSuccess success,
            @Schema(nullable = true) ErrorData error,
            MetaData meta
    ) {
    }

    public record PetSuccess(PetResponse data) {
    }

    @Schema(name = "PetListApiResponse")
    public record PetListEnvelope(
            @Schema(example = "SUCCESS") ResultType resultType,
            PetListSuccess success,
            @Schema(nullable = true) ErrorData error,
            MetaData meta
    ) {
    }

    public record PetListSuccess(List<PetListResponse> data) {
    }

    @Schema(name = "UserProfileApiResponse")
    public record UserProfileEnvelope(
            @Schema(example = "SUCCESS") ResultType resultType,
            UserProfileSuccess success,
            @Schema(nullable = true) ErrorData error,
            MetaData meta
    ) {
    }

    public record UserProfileSuccess(UserProfileResponse data) {
    }

    @Schema(name = "ErrorApiResponse")
    public record ErrorEnvelope(
            @Schema(example = "FAIL") ResultType resultType,
            @Schema(nullable = true) Object success,
            ErrorData error,
            MetaData meta
    ) {
    }

    public record ErrorData(
            @Schema(description = "오류 코드", example = "VALIDATION_FAILED") String code,
            @Schema(description = "오류 메시지", example = "요청값이 누락되었거나 올바르지 않습니다.") String message,
            @Schema(description = "추가 오류 정보", nullable = true) Object details
    ) {
    }

    public record MetaData(
            @Schema(description = "UTC 응답 시각. ApiResponseBody의 LocalDateTime 직렬화 형식",
                    example = "2026-10-18T07:52:18.222", type = "string", format = "date-time")
            LocalDateTime timestamp,
            @Schema(description = "요청 경로", example = "/api/v1/pets") String path
    ) {
    }
}
