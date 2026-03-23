package com.github.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollaboratorPermissions {

    private boolean admin;
    private boolean push;
    private boolean pull;

    public String resolvePermission() {
        if (admin) return "admin";
        if (push)  return "write";
        if (pull)  return "read";
        return "none";
    }
}