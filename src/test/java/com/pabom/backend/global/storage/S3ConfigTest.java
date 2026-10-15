package com.pabom.backend.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ConfigTest {

    private final S3Config config = new S3Config();
    private final S3Properties properties = new S3Properties("ap-northeast-2", "pabom-test", 3600);

    @Test
    void createsS3ClientForConfiguredRegion() {
        try (S3Client client = config.s3Client(properties)) {
            assertThat(client.serviceClientConfiguration().region())
                    .isEqualTo(Region.AP_NORTHEAST_2);
        }
    }

    @Test
    void createsS3PresignerWithoutStaticCredentials() {
        try (S3Presigner presigner = config.s3Presigner(properties)) {
            assertThat(presigner).isNotNull();
        }
    }
}
