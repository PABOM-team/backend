package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.command.OAuthCallbackCommand;
import com.pabom.backend.auth.application.result.OAuthAuthorizationResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.model.OAuthProvider;

public interface OAuthService {

    OAuthProvider provider();

    OAuthAuthorizationResult issueAuthorizationUrl();

    OAuthLoginResult login(OAuthCallbackCommand command);
}
