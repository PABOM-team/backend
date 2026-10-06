package com.pabom.backend.auth.application.port;

import com.pabom.backend.auth.domain.model.SocialUserInfo;

public interface GoogleOAuthClientPort {

    String createAuthorizationUrl(String state);

    SocialUserInfo authenticate(String authorizationCode);
}
