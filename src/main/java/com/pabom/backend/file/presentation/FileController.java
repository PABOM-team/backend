package com.pabom.backend.file.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.file.application.command.UploadFileCommand;
import com.pabom.backend.file.application.result.FileDownloadResult;
import com.pabom.backend.file.application.result.FileUploadResult;
import com.pabom.backend.file.application.service.FileQueryService;
import com.pabom.backend.file.application.service.FileUploadService;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.presentation.docs.FileControllerDocs;
import com.pabom.backend.file.presentation.response.FileDownloadResponse;
import com.pabom.backend.file.presentation.response.FileUploadResponse;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController implements FileControllerDocs {

    private static final ZoneId RESPONSE_ZONE = ZoneId.of("Asia/Seoul");

    private final FileUploadService fileUploadService;
    private final FileQueryService fileQueryService;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<FileUploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("purpose") FilePurpose purpose,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        FileUploadResult result = fileUploadService.upload(new UploadFileCommand(
                authUser.userId(),
                purpose,
                file.getContentType(),
                readBytes(file)
        ));
        FileUploadResponse response = new FileUploadResponse(
                result.id(),
                result.purpose(),
                result.contentType(),
                result.sizeBytes(),
                result.width(),
                result.height(),
                result.url(),
                result.status(),
                result.tempExpiresAt().atZone(RESPONSE_ZONE).toOffsetDateTime()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseBody.success(response, request));
    }

    @Override
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponseBody<FileDownloadResponse>> get(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        FileDownloadResult result = fileQueryService.get(fileId, authUser.userId());
        FileDownloadResponse response = new FileDownloadResponse(
                result.url(),
                result.expiresAt().atZone(RESPONSE_ZONE).toOffsetDateTime(),
                result.contentType()
        );
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED, exception);
        }
    }

    private void requireAuthentication(AuthUser authUser) {
        if (authUser == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
