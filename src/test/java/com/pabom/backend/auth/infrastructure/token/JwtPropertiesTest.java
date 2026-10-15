package com.pabom.backend.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankSecret() {
        JwtProperties properties = propertiesWithSecret(" ");

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("secret");
    }

    @Test
    void rejectsSecretShorterThan32Characters() {
        JwtProperties properties = propertiesWithSecret("short-secret");

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("secret");
    }

    @Test
    void acceptsSecretWithAtLeast32Characters() {
        JwtProperties properties = propertiesWithSecret("12345678901234567890123456789012");

        assertThat(validator.validate(properties)).isEmpty();
    }

    private JwtProperties propertiesWithSecret(String secret) {
        return new JwtProperties(secret, 1800, 600, 1209600);
    }
}
