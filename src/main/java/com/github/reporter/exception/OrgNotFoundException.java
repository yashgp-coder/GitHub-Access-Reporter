package com.github.reporter.exception;

import org.springframework.http.HttpStatus;

public class OrgNotFoundException extends GitHubApiException {

    public OrgNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}