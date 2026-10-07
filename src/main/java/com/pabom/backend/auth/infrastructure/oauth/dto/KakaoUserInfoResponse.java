package com.pabom.backend.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    public String nickname() {
        return kakaoAccount == null || kakaoAccount.profile() == null
                ? null
                : kakaoAccount.profile().nickname();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            Profile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String nickname) {
    }
}
