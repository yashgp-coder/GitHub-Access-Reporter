package com.github.reporter.service;

import com.github.reporter.client.GitHubClient;
import com.github.reporter.dto.AccessReportResponse;
import com.github.reporter.dto.RepoDTO;
import com.github.reporter.dto.UserAccessDTO;
import com.github.reporter.model.GitHubCollaborator;
import com.github.reporter.model.GitHubRepo;
import com.github.reporter.util.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitHubService {

    private final GitHubClient gitHubClient;
    private final CacheService cacheService;
    private final ExecutorService executorService;

    public GitHubService(GitHubClient gitHubClient,
                         CacheService cacheService,
                         @Qualifier("githubExecutorService") ExecutorService executorService) {
        this.gitHubClient = gitHubClient;
        this.cacheService = cacheService;
        this.executorService = executorService;
    }

    public AccessReportResponse generateAccessReport(String org) {
        log.info("=== Starting access report for org: {} ===", org);
        long startTime = System.currentTimeMillis();

        // Step 1 — Check cache
        AccessReportResponse cached = cacheService.get(org);
        if (cached != null) {
            log.info("Returning cached report for org: {}", org);
            return cached;
        }

        // Step 2 — Fetch repos
        log.info("Fetching repositories for org: {}", org);
        List<GitHubRepo> repos = RetryUtil.retry(
            () -> gitHubClient.fetchAllRepos(org),
            "fetchAllRepos:" + org
        );
        log.info("Found {} repositories for org: {}", repos.size(), org);

        // Step 3 — Thread-safe collections for parallel processing
        Map<String, List<RepoDTO>> userRepoMap  = new ConcurrentHashMap<>();
        List<String>               failedRepos  = new CopyOnWriteArrayList<>(); // ✅ thread-safe

        processReposInParallel(org, repos, userRepoMap, failedRepos);

        // Step 4 — Build response
        List<UserAccessDTO> userAccessList = buildUserAccessList(userRepoMap);
        String status = failedRepos.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS";

        AccessReportResponse response = AccessReportResponse.builder()
                .organization(org)
                .totalUsers(userAccessList.size())
                .totalRepos(repos.size())
                .status(status)
                .generatedAt(LocalDateTime.now())
                .failedRepositories(failedRepos.isEmpty() ? null : List.copyOf(failedRepos))
                .data(userAccessList)
                .build();

        // Step 5 — Only cache valid responses
        if (!repos.isEmpty()) {
            cacheService.put(org, response);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== Done | Org: {} | Users: {} | Repos: {} | Failed: {} | Time: {}ms ===",
            org, userAccessList.size(), repos.size(), failedRepos.size(), elapsed);

        return response;
    }

    private void processReposInParallel(
            String org,
            List<GitHubRepo> repos,
            Map<String, List<RepoDTO>> userRepoMap,
            List<String> failedRepos) {

        log.info("Launching {} parallel tasks for org: {}", repos.size(), org);

        List<CompletableFuture<Void>> futures = repos.stream()
            .map(repo -> CompletableFuture.runAsync(
                () -> processRepo(org, repo, userRepoMap, failedRepos),
                executorService
            ))
            .collect(Collectors.toList());

        try {
            CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
            log.info("All tasks completed for org: {}", org);

        } catch (TimeoutException ex) {
            log.warn("Timeout for org: {} — returning partial results", org);
            futures.forEach(f -> f.cancel(true));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            futures.forEach(f -> f.cancel(true));

        } catch (Exception ex) {
            // ExecutionException from individual task failures — already captured in failedRepos
            log.warn("Parallel processing finished with errors for org: {} — {}",
                org, ex.getMessage());
        }
    }

    private void processRepo(
            String org,
            GitHubRepo repo,
            Map<String, List<RepoDTO>> userRepoMap,
            List<String> failedRepos) {

        String repoName = repo.getName();

        try {
            List<GitHubCollaborator> collaborators = RetryUtil.retry(
                () -> gitHubClient.fetchCollaborators(org, repoName),
                "fetchCollaborators:" + org + "/" + repoName
            );

            for (GitHubCollaborator collaborator : collaborators) {
                RepoDTO repoDTO = RepoDTO.builder()
                        .repoName(repoName)
                        .permission(resolvePermission(collaborator))
                        .build();

                // ✅ ConcurrentHashMap + CopyOnWriteArrayList = fully thread-safe
                userRepoMap.computeIfAbsent(
                    collaborator.getUsername(),
                    k -> new CopyOnWriteArrayList<>()
                ).add(repoDTO);
            }

        } catch (Exception ex) {
            log.error("Failed repo {}/{}: {}", org, repoName, ex.getMessage());
            failedRepos.add(repoName); // ✅ CopyOnWriteArrayList — no synchronized needed
        }
    }

    private String resolvePermission(GitHubCollaborator collaborator) {
        if (collaborator.getPermissions() != null) {
            return collaborator.getPermissions().resolvePermission();
        }
        return "read";
    }

    private List<UserAccessDTO> buildUserAccessList(Map<String, List<RepoDTO>> userRepoMap) {
        return userRepoMap.entrySet().stream()
                .map(entry -> UserAccessDTO.builder()
                        .username(entry.getKey())
                        .repositories(entry.getValue())
                        .build())
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .collect(Collectors.toList());
    }
}