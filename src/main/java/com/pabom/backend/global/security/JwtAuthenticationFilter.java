package com.pabom.backend.global.security;

import com.pabom.backend.auth.application.port.ServiceTokenPort;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE =
            JwtAuthenticationFilter.class.getName() + ".AUTH_ERROR";

    private static final String BEARER_PREFIX = "Bearer ";

    private final ServiceTokenPort tokenProvider;

    public JwtAuthenticationFilter(ServiceTokenPort tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            if (!authorization.startsWith(BEARER_PREFIX)) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_INVALID);
            } else {
                String token = authorization.substring(BEARER_PREFIX.length()).trim();
                if (StringUtils.hasText(token)) {
                    authenticate(request, token);
                } else {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_INVALID);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Long userId = tokenProvider.validateAccessToken(token);
            AuthUser principal = new AuthUser(userId);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, List.of())
            );
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            // Controllers that require authentication can preserve the exact JWT failure reason.
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, exception.errorCode());
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
    }
}
