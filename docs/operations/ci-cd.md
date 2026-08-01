# Vetra Backend Core — Enterprise CI/CD Pipeline Documentation

This document outlines the architecture, workflow execution, quality gates, security scanning, and branch protection configurations for the **Vetra Backend Platform** Continuous Integration pipeline.

---

## 1. Pipeline Architecture Overview

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
 │ • Java 21     │          │ • Buildx      │          │ • Gitleaks    │
 │ • Maven Cache │          │ • Dockerfile  │          │ • API Keys    │
 │ • Checkstyle  │          │ • Compose Conf│          │ • Passwords   │
 │ • Unit Tests  │          └───────────────┘          └───────────────┘
 │ • Integr.Test │
 │ • Upload JAR  │
 └───────────────┘
         │
         ▼
 ┌───────────────┐
 │ CodeQL SAST   │
 │ Security Scan │
 └───────────────┘
```

---

## 2. GitHub Actions Workflows

### A. Main CI Workflow (`.github/workflows/ci.yml`)

1. **`build-and-test` Job:**
   - JDK: Eclipse Temurin Java 21.
   - Cache: Automated Maven dependency layer caching (`cache: 'maven'`).
   - Command: `./mvnw clean verify -B`.
   - Quality Verification: Fails if Checkstyle violations, compilation errors, or unit/integration test failures occur.
   - Artifact Upload: Saves `target/vetra-backend-*.jar` and `target/surefire-reports/` with a **30-day retention period**.

2. **`docker-validation` Job:**
   - Setup Docker Buildx.
   - Validates that canonical multi-stage `Dockerfile` builds cleanly without pushing to a registry.
   - Validates syntax of `docker-compose.yml` via `docker compose config`.

3. **`secret-scan` Job:**
   - Runs Gitleaks (`gitleaks/gitleaks-action@v2`) across entire git commit history to prevent accidental exposure of JWT secrets, database credentials, or API keys.

---

### B. CodeQL Security SAST Workflow (`.github/workflows/codeql.yml`)

- Runs static application security testing (SAST) using GitHub CodeQL for Java.
- Detects SQL injection risk, OWASP Top 10 vulnerabilities, insecure cryptographic primitives, and resource leaks.

---

### C. Dependabot Dependency Updates (`.github/dependabot.yml`)

- Automatically checks for weekly updates to Maven dependencies and GitHub Actions.
- Opens pull requests every Monday at 04:00 UTC.

---

## 3. Mandatory Quality Gates

A Pull Request **cannot be merged** into `main` unless all of the following quality gates pass:

1. **Checkstyle Compliance:** Zero style violations under Google Java Style ruleset.
2. **Compilation Cleanliness:** Zero Java 21 compilation errors or warnings.
3. **Test Suite 100% Pass Rate:** All unit and integration test suites pass without failure.
4. **Docker Validation:** Multi-stage container build executes cleanly.
5. **Zero Secret Leaks:** Gitleaks confirms zero exposed credentials.
6. **CodeQL Security Pass:** Zero critical SAST security alerts.

---

## 4. GitHub Branch Protection Setup Guide

To enforce these quality gates in the GitHub repository settings:

1. Navigate to **Settings** → **Branches** → **Add branch protection rule**.
2. Set **Branch name pattern** to `main`.
3. Enable **Require a pull request before merging**.
4. Enable **Require status checks to pass before merging**:
   - Search and select: `Build, Checkstyle & Test Suite Execution`
   - Search and select: `Validate Production Docker & Compose Infrastructure`
   - Search and select: `Gitleaks Secret & API Key Exposure Scanner`
   - Search and select: `Analyze Java Code Quality & Security (CodeQL)`
5. Enable **Require branches to be up to date before merging**.
6. Enable **Do not allow bypassing the above settings**.
