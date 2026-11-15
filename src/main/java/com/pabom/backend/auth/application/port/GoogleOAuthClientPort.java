package com.pabom.backend.auth.application.port;

import com.pabom.backend.auth.domain.model.OAuthClientType;
import com.pabom.backend.auth.domain.model.SocialUserInfo;

public interface GoogleOAuthClientPort {

    String createAuthorizationUrl(String state, OAuthClientType clientType);

    SocialUserInfo authenticate(String authorizationCode, OAuthClientType clientType);

    SocialUserInfo authenticateWithIdToken(String idToken);
}
