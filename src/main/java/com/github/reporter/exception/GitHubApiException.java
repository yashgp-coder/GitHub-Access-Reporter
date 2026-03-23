package com.github.reporter.exception;

import org.springframework.http.HttpStatus;

public class GitHubApiException extends RuntimeException {

    private final HttpStatus httpStatus;

    public GitHubApiException(String message) {
        super(message);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public GitHubApiException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}