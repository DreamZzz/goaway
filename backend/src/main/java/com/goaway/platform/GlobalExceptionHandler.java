package com.goaway.platform;

import com.goaway.platform.security.GuestAccessException;
import com.goaway.shared.dto.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation failures (@Valid on request DTOs).
     * Returns 400 with a comma-separated list of field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        // Record the rejection with client IP + target path so suspicious patterns
        // (out-of-range values, injection attempts) can be investigated via logs.
        log.warn("Request validation rejected: method={} path={} remote={} errors=[{}]",
                request.getMethod(),
                request.getRequestURI(),
                clientIp(request),
                errors);
        return ResponseEntity.badRequest().body(new MessageResponse(errors));
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Business rule violations surfaced as IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Static resource misses (e.g. /favicon.ico from browsers/crawlers) — not an application error.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Explicit ResponseStatusException — honour its status code (e.g. 401/404 from admin guard).
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<MessageResponse> handleResponseStatus(
            org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new MessageResponse(ex.getReason() != null ? ex.getReason() : "请求被拒绝"));
    }

    /**
     * 游客额度/上下文异常（如对线试用次数耗尽）——返回其携带的状态码与剩余次数，引导登录。
     */
    @ExceptionHandler(GuestAccessException.class)
    public ResponseEntity<MessageResponse> handleGuestAccess(GuestAccessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new MessageResponse(ex.getMessage()));
    }

    /**
     * Catch-all — prevents HTML error pages leaking to API clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("服务器内部错误，请稍后重试"));
    }
}
