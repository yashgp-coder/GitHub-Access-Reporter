package com.github.reporter.controller;

import com.github.reporter.dto.AccessReportResponse;
import com.github.reporter.dto.ErrorResponse;
import com.github.reporter.service.CacheService;
import com.github.reporter.service.GitHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/github")
@Tag(name = "GitHub Access Report", description = "Generate user-repository access reports for a GitHub organization")
public class GitHubController {

    private final GitHubService gitHubService;
    private final CacheService cacheService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/github/access-report?org={orgName}
    // Main endpoint — returns full access report for an org
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/access-report")
    @Operation(
        summary     = "Generate access report for a GitHub organization",
        description = "Fetches all repositories and collaborators for the given org " +
                      "and returns a mapping of users to repositories with permission levels. " +
                      "Results are cached for 5 minutes."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Report generated successfully",
            content      = @Content(schema = @Schema(implementation = AccessReportResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "Missing or blank org parameter",
            content      = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description  = "Invalid or expired GitHub token",
            content      = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description  = "Organization not found on GitHub",
            content      = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description  = "GitHub API rate limit exceeded",
            content      = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description  = "Internal server error or GitHub API failure",
            content      = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<AccessReportResponse> getAccessReport(
            @Parameter(description = "GitHub organization name", example = "google", required = true)
            @RequestParam @NotBlank(message = "Organization name must not be blank")
            String org) {

        log.info("Received access report request for org: {}", org);

        AccessReportResponse response = gitHubService.generateAccessReport(org.trim());

        log.info("Returning access report for org: {} | status: {} | users: {} | repos: {}",
            org, response.getStatus(), response.getTotalUsers(), response.getTotalRepos());

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/github/cache/evict?org={orgName}
    // Utility endpoint — manually clear cache for an org
    // Useful during testing so you don't wait 5 minutes for TTL
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/cache/evict")
    @Operation(
        summary     = "Evict cached report for an organization",
        description = "Clears the cached access report for the given org. " +
                      "The next call to /access-report will fetch fresh data from GitHub."
    )
    @ApiResponse(responseCode = "200", description = "Cache evicted successfully")
    public ResponseEntity<String> evictCache(
            @Parameter(description = "GitHub organization name", example = "google", required = true)
            @RequestParam @NotBlank(message = "Organization name must not be blank")
            String org) {

        log.info("Cache evict requested for org: {}", org);
        cacheService.evict(org.trim());
        return ResponseEntity.ok("Cache evicted for org: " + org);
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/github/health
    // Simple health check — confirms app is running
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns OK if the service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GitHub Access Reporter is running");
    }
}