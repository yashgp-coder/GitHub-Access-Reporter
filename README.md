# 🚀 GitHub Access Reporter

A scalable Spring Boot backend service that integrates with the GitHub API to generate a **user → repository access report** for any organization or user.

> 🔍 Helps organizations understand who has access to what across repositories.

---

## ✨ Key Features

- 🔗 Fetch all repositories of a GitHub org/user (pagination supported)
- 👥 Retrieve collaborators with permission levels (`admin`, `write`, `read`)
- 📊 Aggregate into **User → Repositories** mapping
- ⚡ Parallel processing using `CompletableFuture + ThreadPoolExecutor`
- 🧠 In-memory caching (TTL-based, default: 5 minutes)
- 🔁 Retry mechanism for transient failures
- 🧩 Partial failure handling (continues even if some repos fail)
- 📄 Clean Swagger/OpenAPI documentation
- 🎨 Optional lightweight frontend (single `index.html`)

---

## 🧠 Architecture Overview

```
Controller → Service → Client → GitHub API
                    ↓
                 Cache
```

| Layer | Responsibility |
|---|---|
| **Controller** | Exposes REST endpoints |
| **Service** | Core business logic + aggregation |
| **Client** | Handles GitHub API calls |
| **CacheService** | Improves performance |

---

## 🛠 Tech Stack

| Tech | Purpose |
|---|---|
| Java 21 | Core language |
| Spring Boot 3 | Backend framework |
| RestTemplate | API calls |
| CompletableFuture | Parallel execution |
| Maven | Build tool |
| Swagger | API docs |
| Lombok | Boilerplate reduction |

---

## 📁 Project Structure

```
controller/   → REST endpoints
service/      → Core logic
client/       → GitHub API integration
dto/          → API responses
model/        → GitHub data mapping
exception/    → Error handling
config/       → Beans + thread pool
util/         → Retry logic
```

---

## ⚙️ Setup

### 1️⃣ Prerequisites

- ✅ Java 21
- ✅ Maven 3.6+
- ✅ GitHub Personal Access Token

### 2️⃣ Add GitHub Token

In `src/main/resources/application.properties`:

```properties
github.api.token=${GITHUB_TOKEN}
```

Then set the environment variable:

**Windows (PowerShell):**
```powershell
$env:GITHUB_TOKEN="your_token_here"
```

**Linux / Mac:**
```bash
export GITHUB_TOKEN="your_token_here"
```

### ▶️ Run Application

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/github/access-report?org=` | Generate report |
| GET | `/api/github/cache/evict?org=` | Clear cache |
| GET | `/api/github/health` | Health check |

---

## 📊 Example Response

```json
{
  "organization": "google",
  "totalUsers": 1,
  "totalRepos": 2846,
  "status": "SUCCESS",
  "generatedAt": "2026-03-22T12:57:31",
  "data": [
    {
      "username": "user1",
      "repositories": [
        { "repoName": "repo1", "permission": "admin" }
      ]
    }
  ]
}
```

---

## ⚡ Performance & Scalability

✔ Handles:
- ✅ 100+ repositories
- ✅ 1000+ users

Optimizations:
- ✅ Parallel API calls (thread pool)
- ✅ Pagination handling
- ✅ Caching layer (reduces API calls)
- ✅ Retry logic for failures

---

## 🎨 Frontend (index.html)

A dark-themed developer UI — no npm, no build step, no dependencies.

> ⚠️ **Start the backend first** before opening the frontend.

### Method 1 — Open directly in browser

Double-click `index.html` or drag it into any browser window.

```
File → Open File → select index.html
```

The page connects to `localhost:8080` automatically.
If the browser blocks requests due to CORS, use Method 2.

### Method 2 — Serve via Spring Boot *(recommended, zero CORS issues)*

Move `index.html` into:

```
src/main/resources/static/index.html
```

Restart the server, then open:

```
http://localhost:8080
```

The frontend loads from the same origin as the API — no CORS issues at all.

### What the frontend includes

| Feature | Details |
|---|---|
| 🟢 Health check bar | Live status of API server + GitHub API, auto-rechecks every 30s |
| 🔍 Search bar | Type any org or username, press Enter or click Run |
| ⚡ Quick hints | Click `google`, `microsoft` etc to pre-fill |
| 📊 Summary cards | Total repos, users, status, failed count |
| 👤 User cards | Click any user to expand their full repo list |
| 🎨 Permission badges | `admin` / `write` / `read` color coded |
| 🔎 Live filter | Search across users and repo names instantly |
| 🗑️ Cache evict | One-click button to clear cached report |
| 🖼️ GitHub avatars | Auto-loads real GitHub profile pictures |
| ❌ Error handling | Clean messages for 404, 401, 500, server offline |

---

## ⚠️ Known Limitations

- ⚠️ GitHub restricts collaborator visibility → may return partial data for orgs you're not a member of
- ⚠️ Large orgs may hit GitHub rate limits (5000 req/hour)
- ⚠️ Cache is in-memory — resets on restart

---

## 🧠 Design Decisions *(Interview Ready)*

| Decision | Reason |
|---|---|
| **Parallel Processing** | Reduces execution time drastically for large orgs |
| **Cache Layer** | Avoids repeated API calls for same org |
| **Partial Failure Handling** | Improves robustness — one failure doesn't crash the report |
| **Thread-safe collections** | `ConcurrentHashMap` + `CopyOnWriteArrayList` ensure concurrency safety |

---

## 💡 How to Test

**Swagger UI:**
```
http://localhost:8080/swagger-ui.html
```

**Frontend UI:**
```
http://localhost:8080
```
*(after placing `index.html` in `src/main/resources/static/`)*

**Try with your own account:**
```
org = your_github_username
```

---

## 🏆 What Makes This Stand Out

- ✅ Production-style architecture
- ✅ Scalable + fault-tolerant
- ✅ Clean API design
- ✅ Real-world use case

---

## 👨‍💻 Author

**Yash Kesarwani**