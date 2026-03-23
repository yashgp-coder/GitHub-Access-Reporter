package com.github.reporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class GitHubAccessReporterApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubAccessReporterApplication.class, args);
    }
}