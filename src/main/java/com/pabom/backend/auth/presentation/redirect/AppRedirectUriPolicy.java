package com.pabom.backend.auth.presentation.redirect;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.config.AuthAppRedirectProperties;
import com.pabom.backend.global.error.BusinessException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 앱 Redirect URI가 허용 목록과 정확히 일치하는지 검사한다.
 */
@Component
public class AppRedirectUriPolicy {

    private static final String PROVIDER_PLACEHOLDER = "{provider}";

    private final List<String> allowedUris;

    public AppRedirectUriPolicy(AuthAppRedirectProperties properties) {
        this.allowedUris = properties.allowedUris();
    }

    public boolean isAllowed(OAuthProvider provider, String redirectUri) {
        if (redirectUri == null) {
            return false;
        }
        String providerName = provider.name().toLowerCase(Locale.ROOT);
        return allowedUris.stream()
                .map(uri -> uri.trim().replace(PROVIDER_PLACEHOLDER, providerName))
                .anyMatch(redirectUri::equals);
    }

    public void validate(OAuthProvider provider, String redirectUri) {
        if (!isAllowed(provider, redirectUri)) {
            throw new BusinessException(AuthErrorCode.REDIRECT_URI_NOT_ALLOWED);
        }
    }
}
