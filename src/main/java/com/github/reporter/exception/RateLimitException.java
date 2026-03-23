package com.github.reporter.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends GitHubApiException {

    public RateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}