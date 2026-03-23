package com.github.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCollaborator {

    @JsonProperty("login")
    private String username;

    private String type;

    @JsonProperty("site_admin")
    private boolean siteAdmin;

    private CollaboratorPermissions permissions;
}