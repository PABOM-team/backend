package com.pabom.backend.file.presentation;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pabom.backend.file.application.result.FileDownloadResult;
import com.pabom.backend.file.application.result.FileUploadResult;
import com.pabom.backend.file.application.service.FileQueryService;
import com.pabom.backend.file.application.service.FileUploadService;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler;
import com.pabom.backend.global.security.AuthUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FileControllerTest {

    private FileUploadService uploadService;
    private FileQueryService queryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        uploadService = mock(FileUploadService.class);
        queryService = mock(FileQueryService.class);
        FileController controller = new FileController(uploadService, queryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(authentication(7L));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadsMultipartFileWithCommonResponseWrapper() throws Exception {
        given(uploadService.upload(argThat(command ->
                command.ownerId().equals(7L) && command.purpose() == FilePurpose.PET_PHOTO
        ))).willReturn(new FileUploadResult(
                301L,
                FilePurpose.PET_PHOTO,
                "image/jpeg",
                3,
                20,
                10,
                "https://signed.example/file",
                FileStatus.TEMP,
                Instant.parse("2026-10-25T01:00:00Z")
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file", "pet.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("purpose", "PET_PHOTO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.success.data.id").value(301))
                .andExpect(jsonPath("$.success.data.purpose").value("PET_PHOTO"))
                .andExpect(jsonPath("$.success.data.width").value(20))
                .andExpect(jsonPath("$.success.data.height").value(10))
                .andExpect(jsonPath("$.success.data.status").value("TEMP"))
                .andExpect(jsonPath("$.success.data.tempExpiresAt")
                        .value("2026-10-25T10:00:00+09:00"))
                .andExpect(jsonPath("$.meta.path").value("/api/v1/files"));
    }

    @Test
    void returnsOwnedFileUrlAsJsonInsteadOfRedirect() throws Exception {
        given(queryService.get(301L, 7L)).willReturn(new FileDownloadResult(
                "https://signed.example/file",
                Instant.parse("2026-10-24T01:10:00Z"),
                "image/jpeg"
        ));

        mockMvc.perform(get("/api/v1/files/301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success.data.url").value("https://signed.example/file"))
                .andExpect(jsonPath("$.success.data.expiresAt")
                        .value("2026-10-24T10:10:00+09:00"))
                .andExpect(jsonPath("$.success.data.contentType").value("image/jpeg"));

        verify(queryService).get(301L, 7L);
    }

    @Test
    void returnsFileNotFoundContractForHiddenFile() throws Exception {
        given(queryService.get(301L, 7L))
                .willThrow(new BusinessException(FileErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/files/301"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/files/301"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void rejectsMissingMultipartFieldsWithValidationError() throws Exception {
        mockMvc.perform(multipart("/api/v1/files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUnknownPurposeWithValidationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pet.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("purpose", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    private UsernamePasswordAuthenticationToken authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(new AuthUser(userId), null, List.of());
    }
}
