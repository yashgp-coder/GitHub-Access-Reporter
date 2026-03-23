package com.github.reporter.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "github.api")
public class GitHubApiProperties {

    private String baseUrl;
    private String token;
    private int pageSize = 100;
}