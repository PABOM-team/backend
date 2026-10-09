package com.pabom.backend.auth.infrastructure.token;

import com.pabom.backend.auth.application.port.ServiceTokenPort;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ServiceTokenProvider implements ServiceTokenPort {

    private final JwtFactory jwtFactory;
    private final JwtProperties properties;

    public ServiceTokenProvider(JwtFactory jwtFactory, JwtProperties properties) {
        this.jwtFactory = jwtFactory;
        this.properties = properties;
    }

    @Override
    public IssuedToken issueAccessToken(Long userId, Instant issuedAt) {
        return issueJwt(userId, TokenType.ACCESS, properties.accessExpirationSeconds(), issuedAt);
    }

    @Override
    public IssuedToken issueSignupToken(Long userId, Instant issuedAt) {
        return issueJwt(userId, TokenType.SIGNUP, properties.signupExpirationSeconds(), issuedAt);
    }

    @Override
    public IssuedRefreshToken issueRefreshToken(Long userId, Instant issuedAt) {
        Instant expiresAt = issuedAt.plusSeconds(properties.refreshExpirationSeconds());
        String familyId = UUID.randomUUID().toString();
        String token = jwtFactory.create(userId, TokenType.REFRESH, issuedAt, expiresAt);
        return new IssuedRefreshToken(
                token,
                hash(token),
                familyId,
                issuedAt,
                expiresAt,
                properties.refreshExpirationSeconds()
        );
    }

    @Override
    public Long validateSignupToken(String token) {
        return validate(token, TokenType.SIGNUP);
    }

    @Override
    public Long validateAccessToken(String token) {
        return validate(token, TokenType.ACCESS);
    }

    private Long validate(String token, TokenType expectedType) {
        try {
            return validateAndGetUserId(token, expectedType);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN, exception);
        }
    }

    public Long validateAndGetUserId(String token, TokenType expectedType) {
        Claims claims = jwtFactory.parse(token);
        String actualType = claims.get("tokenType", String.class);
        if (!expectedType.name().equals(actualType)) {
            throw new IllegalArgumentException("Unexpected token type");
        }
        return Long.valueOf(claims.getSubject());
    }

    private IssuedToken issueJwt(
            Long userId,
            TokenType tokenType,
            long expirationSeconds,
            Instant issuedAt
    ) {
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);
        return new IssuedToken(
                jwtFactory.create(userId, tokenType, issuedAt, expiresAt),
                expirationSeconds
        );
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

}
