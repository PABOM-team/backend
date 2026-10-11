package com.pabom.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.infrastructure.config.AuthWebProperties;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.response.ApiResponseBody.ErrorBody;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    private final Set<String> allowedOrigins;
    private final ObjectMapper objectMapper;

    public OriginValidationFilter(AuthWebProperties properties, ObjectMapper objectMapper) {
        this.allowedOrigins = Set.copyOf(properties.allowedOrigins());
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        return !PROTECTED_PATHS.contains(requestPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (StringUtils.hasText(origin) && !allowedOrigins.contains(origin)) {
            writeForbiddenResponse(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeForbiddenResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        AuthErrorCode errorCode = AuthErrorCode.ORIGIN_NOT_ALLOWED;
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponseBody.fail(
                        new ErrorBody(errorCode.getCode(), errorCode.getMessage(), null),
                        request
                )
        );
    }
}
