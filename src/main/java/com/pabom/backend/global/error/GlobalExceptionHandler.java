package com.pabom.backend.global.error;

import com.pabom.backend.auth.domain.error.AuthErrorCode;

import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.response.ApiResponseBody.ErrorBody;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseBody<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        BaseErrorCode errorCode = exception.errorCode();
        log.warn(
                "HTTP error response. status={} code={} method={} uri={}",
                errorCode.getStatus().value(),
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI()
        );
        ErrorBody error = new ErrorBody(errorCode.getCode(), exception.getMessage(), null);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponseBody.fail(error, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseBody<Void>> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        BaseErrorCode errorCode = AuthErrorCode.VALIDATION_FAILED;
        ErrorBody error = new ErrorBody(
                errorCode.getCode(),
                errorCode.getMessage(),
                new FieldErrorDetail(exception.getParameterName(), "필수 요청 파라미터입니다.")
        );
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponseBody.fail(error, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseBody<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        BaseErrorCode errorCode = AuthErrorCode.VALIDATION_FAILED;
        Object details = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> new FieldErrorDetail(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .orElse(null);
        ErrorBody error = new ErrorBody(errorCode.getCode(), errorCode.getMessage(), details);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponseBody.fail(error, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseBody<Void>> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        BaseErrorCode errorCode = AuthErrorCode.VALIDATION_FAILED;
        ErrorBody error = new ErrorBody(errorCode.getCode(), errorCode.getMessage(), null);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponseBody.fail(error, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseBody<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception occurred. uri={}", request.getRequestURI(), exception);
        BaseErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        ErrorBody error = new ErrorBody(errorCode.getCode(), errorCode.getMessage(), null);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponseBody.fail(error, request));
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
