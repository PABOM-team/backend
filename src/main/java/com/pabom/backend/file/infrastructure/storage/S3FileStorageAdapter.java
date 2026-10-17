package com.pabom.backend.file.infrastructure.storage;

import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.global.storage.S3Properties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    @Override
    public void upload(String storageKey, String contentType, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
    }

    @Override
    public String createDownloadUrl(String storageKey, Duration validity) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(validity)
                .getObjectRequest(objectRequest)
                .build();
        return s3Presigner.presignGetObject(request).url().toExternalForm();
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build());
    }
}
