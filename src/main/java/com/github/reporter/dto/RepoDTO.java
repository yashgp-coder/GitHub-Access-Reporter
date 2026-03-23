package com.github.reporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A repository and the permission level a user has on it")
public class RepoDTO {

    @Schema(description = "Name of the repository", example = "backend-service")
    private String repoName;

    @Schema(description = "Permission level: admin / write / read", example = "write")
    private String permission;
}