package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.command.OAuthTokenLoginCommand;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.model.OAuthProvider;

public interface OAuthService {

    OAuthProvider provider();

    /**
     * @param appRedirectUri 앱 로그인이면 허용 목록 검증을 마친 앱 주소, 웹 로그인이면 null
     */
    OAuthAuthorizationResult issueAuthorizationUrl(String appRedirectUri);

    OAuthLoginResult login(OAuthCallbackCommand command);

    OAuthLoginResult loginWithToken(OAuthTokenLoginCommand command);
}
