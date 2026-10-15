package com.pabom.backend.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class S3PropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankRegionAndBucket() {
        S3Properties properties = new S3Properties(" ", " ", 3600);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("region", "bucket");
    }

    @Test
    void rejectsExpirationLongerThanSevenDays() {
        S3Properties properties = new S3Properties("ap-northeast-2", "pabom-dev", 604801);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("presignedUrlExpirationSeconds");
    }

    @Test
    void acceptsValidConfiguration() {
        S3Properties properties = new S3Properties("ap-northeast-2", "pabom-dev", 3600);

        assertThat(validator.validate(properties)).isEmpty();
    }
}
