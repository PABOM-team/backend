package com.pabom.backend.user.application.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TemporaryNicknameGenerator {

    private static final String PREFIX = "파봄 사용자";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return PREFIX + String.format("%04d", secureRandom.nextInt(10_000));
    }
}
