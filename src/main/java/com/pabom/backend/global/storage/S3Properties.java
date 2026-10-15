package com.pabom.backend.global.storage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.aws.s3")
public record S3Properties(
        @NotBlank String region,
        @NotBlank String bucket,
        @Positive @Max(604800) long presignedUrlExpirationSeconds
) {
}
