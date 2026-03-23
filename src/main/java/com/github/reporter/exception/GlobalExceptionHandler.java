package com.github.reporter.exception;

import com.github.reporter.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 401 — Bad token ──────────────────────────────────────────
    @ExceptionHandler(GitHubAuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            GitHubAuthException ex, HttpServletRequest request) {

        log.error("Auth error on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "GITHUB_AUTH_ERROR", ex.getMessage(), request);
    }

    // ── 404 — Org not found ───────────────────────────────────────
    @ExceptionHandler(OrgNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrgNotFound(
            OrgNotFoundException ex, HttpServletRequest request) {

        log.error("Org not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", ex.getMessage(), request);
    }

    // ── 429 — Rate limit hit ──────────────────────────────────────
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            RateLimitException ex, HttpServletRequest request) {

        log.error("Rate limit hit on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), request);
    }

    // ── 500 — Generic GitHub API error ────────────────────────────
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ErrorResponse> handleGitHubApiException(
            GitHubApiException ex, HttpServletRequest request) {

        log.error("GitHub API error on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "GITHUB_API_ERROR", ex.getMessage(), request);
    }

    // ── 400 — Missing required request param ─────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("Missing param on {}: {}", request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, request);
    }

    // ── 500 — Any unexpected error ────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // TEMPORARY — log full stack trace so we can see root cause
        log.error("Unexpected error on {}: {} — cause: {}",
                request.getRequestURI(),
                ex.getMessage(),
                ex.getCause() != null ? ex.getCause().getMessage() : "none",
                ex); // <-- this prints full stack trace to terminal

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                // TEMPORARY — show real message
                ex.getMessage() != null ? ex.getMessage() : "Null exception — check logs",
                request);
    }

    // ─────────────────────────────────────────────────────────────
    // Builds the ErrorResponse and wraps it in a ResponseEntity
    // ─────────────────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String errorCode,
            String message, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}