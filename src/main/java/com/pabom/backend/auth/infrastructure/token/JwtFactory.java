package com.pabom.backend.auth.infrastructure.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtFactory {

    private static final String ISSUER = "pabom";

    private final SecretKey secretKey;

    public JwtFactory(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String create(Long userId, TokenType tokenType, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .setIssuer(ISSUER)
                .setId(UUID.randomUUID().toString())
                .setSubject(userId.toString())
                .claim("tokenType", tokenType.name())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer(ISSUER)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
