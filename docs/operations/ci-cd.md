# Vetra Backend Core — Enterprise CI/CD & Repository Governance

This document outlines the architecture, workflow execution, quality gates, security scanning, CODEOWNERS, and branch protection configurations for the **Vetra Backend Platform** Continuous Integration pipeline.

---

## 1. Pipeline Architecture & Workflow Modernization

The CI pipeline runs automatically on every `push` to `main` or `feature/**` branches, as well as every `pull_request` targetting `main`.

```
                        ┌────────────────────────┐
                        │ Push / Pull Request    │
                        └───────────┬────────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────┐
         ▼                          ▼                          ▼
 ┌───────────────┐          ┌───────────────┐          ┌───────────────┐
 │ build-and-test│          │docker-validatn│          │ secret-scan   │
 │               │          │               │          │               │
 │ • Java 21     │          │ • Buildx GHA  │          │ • Gitleaks    │
 │ • Maven Cache │          │   Layer Cache │          │ • API Keys    │
 │ • Checkstyle  │          │ • Dockerfile  │          │ • Passwords   │
 │ • Unit Tests  │          │ • Compose Conf│          │ • .gitleaks.  │
 │ • Integr.Test │          └───────────────┘            toml          │
 │ • Upload JAR  │                                     └───────────────┘
 └───────────────┘
         │
         ▼
 ┌───────────────┐
 │ CodeQL SAST   │
 │ Security Scan │
 └───────────────┘
```

---

## 2. GitHub Actions Workflows & Action Versions

All workflows utilize official, actively maintained actions targeting LTS runner environments:

| Workflow Step | Action Component | Major Version | Optimizations |
|---|---|---|---|
| Repository Checkout | `actions/checkout` | `@v4` | Full fetch depth for Gitleaks scanning |
| Java Runtime Setup | `actions/setup-java` | `@v4` | Eclipse Temurin JDK 21 with Maven caching |
| Artifact Storage | `actions/upload-artifact` | `@v4` | 30-day retention for JARs & Surefire reports |
| Docker Build Validation | `docker/build-push-action` | `@v6` | Docker GHA layer caching (`cache-from: type=gha`) |
| Secret Scanning | `gitleaks/gitleaks-action` | `@v2` | Rule overrides configured in `.gitleaks.toml` |
| CodeQL SAST Analysis | `github/codeql-action` | `@v3` | Scheduled & event-driven Java SAST analysis |

---

## 3. Performance & Security Hardening

1. **Concurrency Control:** Each workflow specifies `concurrency: cancel-in-progress: true` to immediately terminate outdated build runs when new commits are pushed to a feature branch.
2. **Execution Timeouts:** All jobs specify explicit `timeout-minutes` (e.g. `15m` for test execution, `20m` for CodeQL) preventing hung jobs from consuming runner minutes.
3. **Least-Privilege Job Permissions:** Workflows define strict top-level and job-level `permissions` (`contents: read`, `checks: write`, `security-events: write`).
4. **Node.js Runner Migration Notice:** GitHub-managed runners periodically issue transition warnings during the GitHub infrastructure rollout from Node 20 to Node 24. Action dependencies in `.github/workflows/` track the latest major versions (`@v4`, `@v6`, `@v3`) to maintain forward compatibility.

---

## 4. Repository Governance & Code Ownership

- **CODEOWNERS (`.github/CODEOWNERS`):** Automatically assigns code review responsibility for pull requests across core architecture, documentation, Docker manifests, and bounded contexts.
- **Issue Templates (`.github/ISSUE_TEMPLATE/`):** Structured YAML templates for Bug Reports and Feature Requests.
- **Pull Request Template (`.github/PULL_REQUEST_TEMPLATE.md`):** Engineering compliance checklist covering Checkstyle, unit tests, OpenAPI annotations, Flyway migrations, and security assertions.

---

## 5. Mandatory Branch Protection Configuration

To enforce these quality gates in GitHub repository settings:

1. Navigate to **Settings** → **Branches** → **Add branch protection rule**.
2. Set **Branch name pattern** to `main`.
3. Enable **Require a pull request before merging**.
4. Enable **Require status checks to pass before merging**:
   - `Build, Checkstyle & Test Suite Execution`
   - `Validate Production Docker & Compose Infrastructure`
   - `Gitleaks Secret & API Key Exposure Scanner`
   - `Analyze Java Code Quality & Security (CodeQL)`
5. Enable **Require branches to be up to date before merging**.
6. Enable **Do not allow bypassing the above settings**.
