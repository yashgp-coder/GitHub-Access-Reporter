package com.github.reporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structured error response returned on API failures")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Short error code", example = "ORG_NOT_FOUND")
    private String error;

    @Schema(description = "Human-readable error message", example = "Organization 'my-org' not found on GitHub")
    private String message;

    @Schema(description = "The API path that triggered the error", example = "/api/github/access-report")
    private String path;

    @Schema(description = "Timestamp of the error")
    private LocalDateTime timestamp;
}