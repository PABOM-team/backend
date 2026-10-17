package com.pabom.backend.file.presentation.request;

import com.pabom.backend.file.domain.type.FilePurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "파일 업로드 multipart 요청")
public record FileUploadRequest(
        @Schema(type = "string", format = "binary", description = "JPEG 또는 PNG 이미지", requiredMode = Schema.RequiredMode.REQUIRED)
        MultipartFile file,
        @Schema(description = "파일 용도. 현재 PET_PHOTO, RECORD_PHOTO만 업로드 가능", requiredMode = Schema.RequiredMode.REQUIRED)
        FilePurpose purpose
) {
}
