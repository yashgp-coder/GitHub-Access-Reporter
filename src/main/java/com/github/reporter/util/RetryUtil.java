package com.github.reporter.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class RetryUtil {

    private static final int MAX_ATTEMPTS = 3;
    private static final long DELAY_MS = 1000; // 1 second between retries

    // ─────────────────────────────────────────────────────────────
    // Generic retry wrapper — wraps ANY supplier with retry logic
    // Usage: RetryUtil.retry(() -> gitHubClient.fetchAllRepos(org))
    // ─────────────────────────────────────────────────────────────
    public static <T> T retry(Supplier<T> action, String actionName) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_ATTEMPTS) {
            try {
                attempt++;
                log.debug("Attempt {}/{} for: {}", attempt, MAX_ATTEMPTS, actionName);
                return action.get();

            } catch (Exception ex) {
                lastException = ex;
                log.warn("Attempt {}/{} failed for '{}': {}",
                    attempt, MAX_ATTEMPTS, actionName, ex.getMessage());

                // Don't retry on auth errors or not-found — retrying won't help
                if (isNonRetryable(ex)) {
                    log.error("Non-retryable error for '{}', stopping immediately: {}",
                        actionName, ex.getMessage());
                    throw ex;
                }

                // If we still have attempts left, wait before retrying
                if (attempt < MAX_ATTEMPTS) {
                    log.info("Waiting {}ms before retry {}/{} for: {}",
                        DELAY_MS, attempt + 1, MAX_ATTEMPTS, actionName);
                    sleep(DELAY_MS);
                }
            }
        }

        // All attempts exhausted
        log.error("All {} attempts failed for: {}", MAX_ATTEMPTS, actionName);
        throw new RuntimeException(
            "Failed after " + MAX_ATTEMPTS + " attempts for: " + actionName,
            lastException
        );
    }

    // ─────────────────────────────────────────────────────────────
    // These errors won't get better with a retry — fail fast
    // ─────────────────────────────────────────────────────────────
    private static boolean isNonRetryable(Exception ex) {
        String exClassName = ex.getClass().getSimpleName();
        return exClassName.equals("GitHubAuthException")
            || exClassName.equals("OrgNotFoundException");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}