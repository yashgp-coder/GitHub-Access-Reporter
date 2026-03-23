package com.github.reporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full access report for a GitHub organization")
public class AccessReportResponse {

    @Schema(description = "GitHub organization name", example = "my-org")
    private String organization;

    @Schema(description = "Total number of unique users found", example = "42")
    private int totalUsers;

    @Schema(description = "Total number of repositories scanned", example = "18")
    private int totalRepos;

    @Schema(
        description = "Report status: SUCCESS if all repos fetched, PARTIAL_SUCCESS if some failed",
        example = "SUCCESS"
    )
    private String status;

    @Schema(description = "Timestamp when the report was generated")
    private LocalDateTime generatedAt;

    @Schema(description = "List of repo names that failed to fetch collaborators (if any)")
    private List<String> failedRepositories;

    @Schema(description = "User → repositories access mapping")
    private List<UserAccessDTO> data;
}