package cn.lims.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> badCredentials(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTH_BAD_CREDENTIALS", "用户名或密码错误", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField() + error.getDefaultMessage())
                .orElse("请求参数不正确");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> business(IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_BUSINESS_RULE", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        String traceId = (String) request.getAttribute("traceId");
        if (traceId == null) traceId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(code, message, traceId, OffsetDateTime.now()));
    }
}

