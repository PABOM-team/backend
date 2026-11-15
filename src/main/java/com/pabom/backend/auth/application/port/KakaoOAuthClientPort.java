package com.pabom.backend.auth.application.port;

import com.pabom.backend.auth.domain.model.OAuthClientType;
import com.pabom.backend.auth.domain.model.SocialUserInfo;

public interface KakaoOAuthClientPort {

    String createAuthorizationUrl(String state, OAuthClientType clientType);

    SocialUserInfo authenticate(String authorizationCode, OAuthClientType clientType);

    SocialUserInfo authenticateWithAccessToken(String accessToken);
}
