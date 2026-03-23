package com.github.reporter.exception;

import org.springframework.http.HttpStatus;

public class GitHubAuthException extends GitHubApiException {

    public GitHubAuthException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}