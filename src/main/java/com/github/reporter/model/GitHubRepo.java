package com.github.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepo {

    private Long id;

    // GitHub returns "name" for repo name
    @JsonProperty("name")
    private String name;

    // Full name = "org/repo-name"
    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("private")
    private boolean isPrivate;

    @JsonProperty("html_url")
    private String htmlUrl;

    private String description;

    @JsonProperty("default_branch")
    private String defaultBranch;
}