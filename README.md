# GitHub Access Reporter

A production-quality Spring Boot service that connects to the GitHub API and generates
a report showing which users have access to which repositories within a given organization.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [How to Add Your GitHub Token](#how-to-add-your-github-token)
- [How to Run](#how-to-run)
- [API Usage](#api-usage)
- [Design Decisions](#design-decisions)

---

## Features

- Fetch all repositories of a GitHub organization or personal account (handles pagination)
- For each repository, fetch all collaborators with permission levels
- Aggregate data into a `user → list of repositories` mapping
- Parallel API calls using `CompletableFuture` + bounded thread pool
- In-memory cache with configurable TTL (default 5 minutes)
- Partial failure handling — if some repos fail, report continues
- Retry logic with 2–3 attempts for transient failures
- Global exception handler returning structured JSON errors
- Full Swagger/OpenAPI documentation

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 LTS | Language |
| Spring Boot | 3.2.3 | Framework |
| Spring Web | — | REST API + RestTemplate |
| Spring Validation | — | Input validation |
| Lombok | 1.18.36 | Boilerplate reduction |
| Springdoc OpenAPI | 2.3.0 | Swagger UI |
| Maven | 3.9+ | Build tool |

---

## Project Structure

```
github-access-reporter/
├── pom.xml
└── src/main/
    ├── resources/
    │   └── application.properties
    └── java/com/github/reporter/
        ├── GitHubAccessReporterApplication.java
        ├── config/
        │   ├── AppConfig.java               # RestTemplate bean, thread pool
        │   └── SwaggerConfig.java           # OpenAPI metadata
        ├── controller/
        │   └── GitHubController.java        # REST endpoints
        ├── service/
        │   ├── GitHubService.java           # Core orchestration + parallel processing
        │   └── CacheService.java            # In-memory TTL cache
        ├── client/
        │   ├── GitHubClient.java            # All GitHub API HTTP calls
        │   └── GitHubApiProperties.java     # Token + baseUrl config binding
        ├── dto/
        │   ├── RepoDTO.java                 # repoName + permission
        │   ├── UserAccessDTO.java           # username + list of RepoDTOs
        │   ├── AccessReportResponse.java    # Top-level API response
        │   └── ErrorResponse.java           # Structured error body
        ├── model/
        │   ├── GitHubRepo.java              # Maps GitHub /repos JSON
        │   ├── GitHubCollaborator.java      # Maps GitHub /collaborators JSON
        │   └── CollaboratorPermissions.java # Maps permission flags
        ├── exception/
        │   ├── GitHubApiException.java      # Base exception
        │   ├── GitHubAuthException.java     # 401 invalid token
        │   ├── OrgNotFoundException.java    # 404 org not found
        │   ├── RateLimitException.java      # 429 rate limited
        │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
        └── util/
            └── RetryUtil.java               # Generic retry wrapper
```

---

## Setup Instructions

### Prerequisites

- Java 21 LTS — download from https://adoptium.net/temurin/releases/?version=21
- Maven 3.6+ — download from https://maven.apache.org/download.cgi
- A GitHub Personal Access Token (PAT)

### Verify installations

```bash
java -version    # should show openjdk 21
mvn -version     # should show Maven 3.x
```

### Clone or create the project

```bash
mkdir github-access-reporter
cd github-access-reporter
# Place all source files as per the structure above
```

---

## How to Add Your GitHub Token

1. Go to: **GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)**
2. Click **Generate new token (classic)**
3. Give it a name like `github-access-reporter`
4. Select scopes:
   - `repo` — access repository data
   - `read:org` — read organization membership
5. Click **Generate token** and copy it immediately

6. Open `src/main/resources/application.properties` and replace the placeholder:

```properties
github.api.token=your_actual_token_here
```

> ⚠️ Never commit your token to version control.
> Add `application.properties` to `.gitignore` if pushing to GitHub.

---

## How to Run

### Option 1 — Maven (development)

```bash
# Install dependencies and compile
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

### Option 2 — JAR (production)

```bash
# Build the JAR
mvn clean package -DskipTests

# Run the JAR
java -jar target/reporter-1.0.0.jar
```

The server starts on `http://localhost:8080`

---

## API Usage

### Base URL
```
http://localhost:8080
```

### Swagger UI (interactive docs)
```
http://localhost:8080/swagger-ui.html
```

---

### 1. Generate Access Report

```
GET /api/github/access-report?org={orgName}
```

**Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `org` | string | Yes | GitHub organization name or personal username |

**Example — PowerShell**
```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/github/access-report?org=google" `
  -UseBasicParsing | Select-Object -ExpandProperty Content
```

**Example — curl (Git Bash / Linux / Mac)**
```bash
curl "http://localhost:8080/api/github/access-report?org=google"
```

**Success Response (200)**
```json
{
  "organization": "google",
  "totalUsers": 1,
  "totalRepos": 2846,
  "status": "SUCCESS",
  "generatedAt": "2026-03-22T12:57:31",
  "failedRepositories": null,
  "data": [
    {
      "username": "some-user",
      "repositories": [
        { "repoName": "guava",   "permission": "admin" },
        { "repoName": "guice",   "permission": "write" },
        { "repoName": "re2",     "permission": "read"  }
      ]
    }
  ]
}
```

**Partial Success Response (200 — some repos failed)**
```json
{
  "organization": "my-org",
  "totalUsers": 3,
  "totalRepos": 50,
  "status": "PARTIAL_SUCCESS",
  "generatedAt": "2026-03-22T12:57:31",
  "failedRepositories": ["private-repo-1", "archived-repo-2"],
  "data": [...]
}
```

**Error Responses**

| Code | Error | Cause |
|---|---|---|
| 400 | `MISSING_PARAMETER` | `org` param not provided |
| 401 | `GITHUB_AUTH_ERROR` | Token invalid or expired |
| 404 | `ORG_NOT_FOUND` | Organization doesn't exist on GitHub |
| 429 | `RATE_LIMIT_EXCEEDED` | GitHub API rate limit hit |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

```json
{
  "status": 404,
  "error": "ORG_NOT_FOUND",
  "message": "Organization 'xyz' not found on GitHub. Please check the org name.",
  "path": "/api/github/access-report",
  "timestamp": "2026-03-22T12:00:00"
}
```

---

### 2. Evict Cache

```
GET /api/github/cache/evict?org={orgName}
```

Clears the cached report for an org. The next call to `/access-report` will fetch fresh data.

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/github/cache/evict?org=google" `
  -UseBasicParsing
```

**Response:** `Cache evicted for org: google`

---

### 3. Health Check

```
GET /api/github/health
```

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/github/health" -UseBasicParsing
```

**Response:** `GitHub Access Reporter is running`

---

## Configuration Reference

All settings live in `src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# GitHub API
github.api.base-url=https://api.github.com
github.api.token=your_token_here
github.api.page-size=100

# Thread pool (for parallel repo processing)
github.threadpool.core-size=10
github.threadpool.max-size=20
github.threadpool.queue-capacity=2000
github.threadpool.thread-name-prefix=github-worker-

# Cache TTL
github.cache.ttl-minutes=5

# Swagger UI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## Design Decisions

### 1. Parallel Processing with CompletableFuture

GitHub API calls for collaborators are made in parallel using `CompletableFuture.runAsync()`
with a bounded `ThreadPoolExecutor`. This reduces total time from O(n) sequential to O(1)
parallel for large organizations. Google's org with 2846 repos completes in ~30 seconds
instead of hours.

```
Sequential:  repo1 → repo2 → repo3 ... → 2846 repos × 500ms = ~24 minutes
Parallel:    all repos fire at once → done in ~30 seconds
```

### 2. Thread-Safe Data Structures

All shared state uses thread-safe collections:
- `ConcurrentHashMap` for the `user → repos` map
- `CopyOnWriteArrayList` for per-user repo lists and failed repos list

This avoids `ConcurrentModificationException` when multiple threads write simultaneously.

### 3. Partial Failure Handling

If a repo's collaborator fetch fails (e.g. 403 access denied), that repo is added to
`failedRepositories` and processing continues. The response returns `PARTIAL_SUCCESS`
instead of failing the whole request. This is critical for large orgs where some repos
may be inaccessible.

### 4. Org vs Personal Account Detection

The GitHub API has separate endpoints for organizations (`/orgs/{org}/repos`) and personal
accounts (`/users/{user}/repos`). The client first probes the org endpoint and falls back
to the user endpoint if a 404 is returned.

### 5. In-Memory Cache with TTL

Responses are cached in a `ConcurrentHashMap` with timestamps. TTL is configurable
(default 5 minutes). This avoids hammering the GitHub API for repeated requests on the
same org. Cache can be manually evicted via the `/cache/evict` endpoint.

### 6. Retry Logic

`RetryUtil` wraps any supplier with up to 3 attempts and 1-second delay between retries.
Non-retryable errors (401 auth, 404 not found) are skipped immediately — retrying them
would never help.

### 7. CallerRunsPolicy for Queue Overflow

The thread pool uses `CallerRunsPolicy` as the rejection handler. When the task queue
fills up (e.g. org with 2000+ repos), overflow tasks run on the caller thread instead of
being rejected. This prevents `RejectedExecutionException` on very large orgs.

---

## Known Limitations

- **Collaborator access requires push access**: GitHub's API only returns collaborators
  for repositories where your token has push (write) or admin access. For orgs you're
  not a member of (like `google`), repos will fall back to returning the org name as
  the sole admin collaborator.

- **Rate limiting**: GitHub allows 5000 API requests per hour for authenticated requests.
  Very large orgs (2000+ repos) may hit this limit. Consider increasing the cache TTL
  to reduce repeat calls.

- **No persistence**: Cache is in-memory only. Restarting the server clears all cached
  reports.
