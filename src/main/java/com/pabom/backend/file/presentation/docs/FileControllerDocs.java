package com.pabom.backend.file.presentation.docs;

import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.presentation.request.FileUploadRequest;
import com.pabom.backend.file.presentation.response.FileDownloadResponse;
import com.pabom.backend.file.presentation.response.FileUploadResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "비공개 파일 업로드 및 조회 API")
public interface FileControllerDocs {

    @Operation(
            summary = "파일 업로드",
            description = "JPEG/PNG 사진을 TEMP 상태로 업로드합니다. PET_PHOTO와 RECORD_PHOTO만 지원합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = FileUploadRequest.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "업로드 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseBody.class),
                            examples = @ExampleObject(name = "업로드 성공", value = UPLOAD_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400",
                    description = "INVALID_FILE_FORMAT, UNSUPPORTED_FILE_PURPOSE, VALIDATION_FAILED",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "잘못된 파일 형식", value = INVALID_FORMAT_EXAMPLE),
                            @ExampleObject(name = "미지원 파일 용도", value = UNSUPPORTED_PURPOSE_EXAMPLE),
                            @ExampleObject(name = "필수 필드 누락", value = VALIDATION_FAILED_EXAMPLE)
                    })),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN: 인증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "인증 실패", value = INVALID_TOKEN_EXAMPLE))),
            @ApiResponse(responseCode = "413", description = "FILE_TOO_LARGE: 사진 크기 10MB 초과",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "파일 크기 초과", value = FILE_TOO_LARGE_EXAMPLE))),
            @ApiResponse(responseCode = "500",
                    description = "FILE_UPLOAD_FAILED: S3 업로드/파일 읽기 실패, INTERNAL_SERVER_ERROR: DB 저장 등 예상하지 못한 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "파일 저장 실패", value = FILE_UPLOAD_FAILED_EXAMPLE),
                            @ExampleObject(name = "서버 내부 오류", value = INTERNAL_SERVER_ERROR_EXAMPLE)
                    }))
    })
    ResponseEntity<ApiResponseBody<FileUploadResponse>> upload(
            @Parameter(description = "업로드할 JPEG/PNG 파일", required = true,
                    schema = @Schema(type = "string", format = "binary")) MultipartFile file,
            @Parameter(description = "PET_PHOTO, RECORD_PHOTO, DEATH_EVIDENCE, REPORT_RECEIPT", required = true)
            FilePurpose purpose,
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "파일 조회 URL 발급",
            description = "소유한 접근 가능 파일에 대해 10분간 유효한 Presigned URL을 JSON으로 반환합니다.",
            security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 URL 발급 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseBody.class),
                            examples = @ExampleObject(name = "조회 성공", value = DOWNLOAD_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "INVALID_TOKEN: 인증 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "인증 실패", value = INVALID_TOKEN_EXAMPLE))),
            @ApiResponse(responseCode = "404",
                    description = "FILE_NOT_FOUND: 파일 없음, 타인 소유 또는 삭제된 파일",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "파일을 찾을 수 없음", value = FILE_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR: URL 발급 등 예상하지 못한 실패",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "서버 내부 오류", value = INTERNAL_SERVER_ERROR_EXAMPLE)))
    })
    ResponseEntity<ApiResponseBody<FileDownloadResponse>> get(
            @Parameter(description = "파일 ID", example = "301", required = true) Long fileId,
            @Parameter(hidden = true) AuthUser authUser,
            @Parameter(hidden = true) HttpServletRequest request
    );

    String UPLOAD_SUCCESS_EXAMPLE = """
            {
              "resultType":"SUCCESS",
              "success":{"data":{"id":301,"purpose":"PET_PHOTO","contentType":"image/jpeg","sizeBytes":812345,"width":2048,"height":1536,"url":"https://storage.example.kr/...","status":"TEMP","tempExpiresAt":"2026-10-25T10:00:00+09:00"}},
              "error":null,
              "meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}
            }
            """;

    String DOWNLOAD_SUCCESS_EXAMPLE = """
            {
              "resultType":"SUCCESS",
              "success":{"data":{"url":"https://storage.example.kr/...","expiresAt":"2026-10-24T10:10:00+09:00","contentType":"image/jpeg"}},
              "error":null,
              "meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files/301"}
            }
            """;

    String INVALID_FORMAT_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"INVALID_FILE_FORMAT","message":"유효한 이미지 파일이 아닙니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
    String UNSUPPORTED_PURPOSE_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"UNSUPPORTED_FILE_PURPOSE","message":"현재 지원하지 않는 파일 용도입니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
    String VALIDATION_FAILED_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"VALIDATION_FAILED","message":"요청값이 누락되었거나 올바르지 않습니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
    String INVALID_TOKEN_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"INVALID_TOKEN","message":"유효하지 않거나 만료된 인증 토큰입니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
    String FILE_TOO_LARGE_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"FILE_TOO_LARGE","message":"파일 크기가 허용 범위를 초과했습니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
    String FILE_UPLOAD_FAILED_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"FILE_UPLOAD_FAILED","message":"파일 저장 중 오류가 발생했습니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
    String FILE_NOT_FOUND_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"FILE_NOT_FOUND","message":"파일을 찾을 수 없습니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files/301"}}
            """;
    String INTERNAL_SERVER_ERROR_EXAMPLE = """
            {"resultType":"FAIL","success":null,"error":{"code":"INTERNAL_SERVER_ERROR","message":"서버 내부 오류가 발생했습니다.","details":null},"meta":{"timestamp":"2026-10-24T01:00:00","path":"/api/v1/files"}}
            """;
}
