package com.pabom.backend.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoResponse(
        String sub,
        String name,
        String email
) {
}
