package com.github.reporter.client;

import com.github.reporter.exception.GitHubAuthException;
import com.github.reporter.exception.GitHubApiException;
import com.github.reporter.exception.OrgNotFoundException;
import com.github.reporter.exception.RateLimitException;
import com.github.reporter.model.CollaboratorPermissions;
import com.github.reporter.model.GitHubCollaborator;
import com.github.reporter.model.GitHubRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final RestTemplate restTemplate;
    private final GitHubApiProperties properties;

    // ─────────────────────────────────────────────────────────────
    // FETCH ALL REPOS FOR AN ORG (handles pagination automatically)
    // GET /orgs/{org}/repos?page=1&per_page=100
    // ─────────────────────────────────────────────────────────────
    public List<GitHubRepo> fetchAllRepos(String org) {
        List<GitHubRepo> allRepos = new ArrayList<>();
        int page = 1;

        log.info("Starting repo fetch for: {}", org);

        // First try org endpoint, fall back to user endpoint if 404
        boolean useUserEndpoint = false;

        // Quick probe to check if it's an org or user
        try {
            String probeUrl = String.format("%s/orgs/%s", properties.getBaseUrl(), org);
            executeGetRequest(probeUrl, Object.class, org);
            log.info("{} is a GitHub Organization", org);
        } catch (OrgNotFoundException ex) {
            log.info("{} is not an org — trying as personal account", org);
            useUserEndpoint = true;
        }

        while (true) {
            String url;
            if (useUserEndpoint) {
                url = String.format(
                        "%s/users/%s/repos?page=%d&per_page=%d",
                        properties.getBaseUrl(), org, page, properties.getPageSize());
            } else {
                url = String.format(
                        "%s/orgs/%s/repos?page=%d&per_page=%d",
                        properties.getBaseUrl(), org, page, properties.getPageSize());
            }

            log.debug("Fetching repos page {} for: {}", page, org);

            GitHubRepo[] pageResult = executeGetRequest(url, GitHubRepo[].class, org);

            if (pageResult == null || pageResult.length == 0) {
                log.info("Finished fetching repos for: {}. Total: {}", org, allRepos.size());
                break;
            }

            allRepos.addAll(Arrays.asList(pageResult));

            if (pageResult.length < properties.getPageSize()) {
                log.info("Last page reached for: {}. Total repos: {}", org, allRepos.size());
                break;
            }

            page++;
        }

        return allRepos;
    }

    // ─────────────────────────────────────────────────────────────
    // FETCH ALL COLLABORATORS FOR A REPO (handles pagination)
    // GET /repos/{org}/{repo}/collaborators?page=1&per_page=100
    // ─────────────────────────────────────────────────────────────
 public List<GitHubCollaborator> fetchCollaborators(String org, String repoName) {
    List<GitHubCollaborator> allCollaborators = new ArrayList<>();
    int page = 1;

    log.debug("Fetching collaborators for repo: {}/{}", org, repoName);

    while (true) {
        String url = String.format(
            "%s/repos/%s/%s/collaborators?page=%d&per_page=%d",
            properties.getBaseUrl(), org, repoName, page, properties.getPageSize()
        );

        GitHubCollaborator[] pageResult;

        try {
            pageResult = executeGetRequest(url, GitHubCollaborator[].class, org);
        } catch (GitHubApiException ex) {
            log.warn("Could not fetch collaborators for {}/{}: {}",
                org, repoName, ex.getMessage());
            // For personal repos — owner always has access, return them as admin
            return buildOwnerAsCollaborator(org);
        }

        if (pageResult == null || pageResult.length == 0) {
            break;
        }

        allCollaborators.addAll(Arrays.asList(pageResult));

        if (pageResult.length < properties.getPageSize()) {
            break;
        }

        page++;
    }

    log.debug("Found {} collaborators for repo: {}/{}", 
        allCollaborators.size(), org, repoName);

    // If empty result, still return owner
    if (allCollaborators.isEmpty()) {
        return buildOwnerAsCollaborator(org);
    }

    return allCollaborators;
}
    // ─────────────────────────────────────────────────────────────
    // CORE HTTP METHOD — builds headers, calls GitHub, maps errors
    // All API calls go through here
    // ─────────────────────────────────────────────────────────────
    private <T> T executeGetRequest(String url, Class<T> responseType, String org) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType);
            return response.getBody();

        } catch (HttpClientErrorException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();

            log.error("GitHub API error: status={}, url={}, message={}",
                    statusCode, url, ex.getMessage());

            // ── 401 — Token is missing, wrong, or expired ──
            if (statusCode == HttpStatus.UNAUTHORIZED) {
                throw new GitHubAuthException(
                        "GitHub token is invalid or expired. " +
                                "Please check your token in application.properties.");
            }

            // ── 403 — Rate limited or insufficient token scope ──
            if (statusCode == HttpStatus.FORBIDDEN) {
                String responseBody = ex.getResponseBodyAsString();
                if (responseBody.contains("rate limit")) {
                    throw new RateLimitException(
                            "GitHub API rate limit exceeded. Please wait and try again.");
                }
                throw new GitHubApiException(
                        "Access forbidden for URL: " + url +
                                ". Your token may lack required scopes (repo, read:org).");
            }

            // ── 404 — Org or repo doesn't exist ──
            if (statusCode == HttpStatus.NOT_FOUND) {
                throw new OrgNotFoundException(
                        "Organization '" + org + "' not found on GitHub. " +
                                "Please check the org name.");
            }

            // ── Everything else (500, 502, etc.) ──
            throw new GitHubApiException(
                    "GitHub API returned error " + statusCode + " for URL: " + url);

        } catch (ResourceAccessException ex) {
            // Network timeout, DNS failure, connection refused
            log.error("Network error calling GitHub API: {}", ex.getMessage());
            throw new GitHubApiException(
                    "Could not reach GitHub API. Check your network connection. Error: "
                            + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // BUILD AUTH HEADERS — token injected from application.properties
    // ─────────────────────────────────────────────────────────────
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + properties.getToken());
        headers.set("Accept", "application/vnd.github+json");
        // GitHub recommends this header for API version stability
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }
    // ─────────────────────────────────────────────────────────────
// For personal repos where collaborator API is blocked,
// treat the owner as the sole admin collaborator
// ─────────────────────────────────────────────────────────────
private List<GitHubCollaborator> buildOwnerAsCollaborator(String username) {
    CollaboratorPermissions permissions = new CollaboratorPermissions();
    permissions.setAdmin(true);

    GitHubCollaborator owner = new GitHubCollaborator();
    owner.setUsername(username);
    owner.setPermissions(permissions);

    log.debug("Built owner collaborator for: {}", username);
    return List.of(owner);
}
}