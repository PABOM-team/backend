package com.pabom.backend.file.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pabom.backend.global.storage.S3Properties;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3FileStorageAdapterTest {

    private S3Client s3Client;
    private S3Presigner presigner;
    private S3FileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        presigner = mock(S3Presigner.class);
        adapter = new S3FileStorageAdapter(
                s3Client,
                presigner,
                new S3Properties("ap-northeast-2", "private-bucket", 600)
        );
    }

    @Test
    void uploadsWithoutPublicAclAndDeletesByStorageKey() {
        adapter.upload("files/1/key.jpg", "image/jpeg", new byte[]{1, 2, 3});
        adapter.delete("files/1/key.jpg");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getValue().bucket()).isEqualTo("private-bucket");
        assertThat(putCaptor.getValue().key()).isEqualTo("files/1/key.jpg");
        assertThat(putCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(putCaptor.getValue().acl()).isNull();

        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().key()).isEqualTo("files/1/key.jpg");
    }

    @Test
    void createsPresignedGetUrlForRequestedTenMinutes() throws Exception {
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        given(signed.url()).willReturn(URI.create("https://signed.example/file").toURL());
        given(presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(signed);

        String url = adapter.createDownloadUrl("files/1/key.jpg", Duration.ofMinutes(10));

        assertThat(url).isEqualTo("https://signed.example/file");
        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("private-bucket");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("files/1/key.jpg");
    }
}
