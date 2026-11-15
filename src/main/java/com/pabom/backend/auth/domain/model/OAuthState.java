package com.pabom.backend.auth.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * OAuth state 값.
 * 웹 로그인은 무작위 값만, 앱 로그인은 "무작위 값.앱 Redirect URI(Base64URL)" 형식으로 앱이 돌아갈 주소를 함께 담는다.
 * 앱 인증 브라우저는 앱의 쿠키 저장소를 공유하지 않으므로, app-callback은 state만으로 돌아갈 앱 주소를 알아야 한다.
 * state 전체가 state 쿠키와 비교되고 앱 주소는 매번 허용 목록으로 다시 검증하므로 변조해도 다른 곳으로 보낼 수 없다.
 */
public record OAuthState(String value, OAuthClientType clientType, String appRedirectUri) {

    private static final char SEPARATOR = '.';

    public static OAuthState web() {
        return new OAuthState(randomValue(), OAuthClientType.WEB, null);
    }

    public static OAuthState app(String appRedirectUri) {
        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(appRedirectUri.getBytes(StandardCharsets.UTF_8));
        return new OAuthState(randomValue() + SEPARATOR + encoded, OAuthClientType.APP, appRedirectUri);
    }

    /**
     * Provider가 돌려준 state를 해석한다. 형식이 맞지 않으면 WEB으로 본다.
     */
    public static OAuthState parse(String value) {
        if (value == null) {
            return new OAuthState(null, OAuthClientType.WEB, null);
        }
        int separatorIndex = value.indexOf(SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == value.length() - 1) {
            return new OAuthState(value, OAuthClientType.WEB, null);
        }
        try {
            String appRedirectUri = new String(
                    Base64.getUrlDecoder().decode(value.substring(separatorIndex + 1)),
                    StandardCharsets.UTF_8
            );
            return new OAuthState(value, OAuthClientType.APP, appRedirectUri);
        } catch (IllegalArgumentException exception) {
            return new OAuthState(value, OAuthClientType.WEB, null);
        }
    }

    private static String randomValue() {
        return UUID.randomUUID().toString();
    }
}
