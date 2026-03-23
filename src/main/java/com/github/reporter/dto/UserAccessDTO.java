package com.github.reporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A GitHub user and all repositories they have access to")
public class UserAccessDTO {

    @Schema(description = "GitHub username", example = "john-doe")
    private String username;

    @Schema(description = "List of repositories this user can access")
    private List<RepoDTO> repositories;
}