package com.pabom.backend.auth.domain.model;

/**
 * OAuth 로그인을 시작한 클라이언트 종류.
 * WEB은 웹 Redirect URI, APP은 앱 전용 Redirect URI(백엔드 app-callback)로 Provider와 통신한다.
 */
public enum OAuthClientType {
    WEB,
    APP
}
