package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.global.error.BusinessException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OAuthServiceResolver {

    private final Map<OAuthProvider, OAuthService> services;

    public OAuthServiceResolver(List<OAuthService> services) {
        this.services = new EnumMap<>(OAuthProvider.class);
        services.forEach(service -> this.services.put(service.provider(), service));
    }

    public OAuthService resolve(OAuthProvider provider) {
        OAuthService service = services.get(provider);
        if (service == null) {
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return service;
    }

    public OAuthService resolve(String providerValue) {
        OAuthProvider provider = Arrays.stream(OAuthProvider.values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(providerValue))
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
        return resolve(provider);
    }
}
