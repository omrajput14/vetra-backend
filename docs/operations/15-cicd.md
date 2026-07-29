# CI/CD Pipeline Documentation
**Document ID:** OPS-15  
**Status:** Planned  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra-backend`, `omrajput14/vetra`  
**References:** [Git Workflow](../engineering/13-git-workflow.md), [Deployment Architecture](../architecture/09-deployment.md), [Testing Strategy](../guides/14-testing-strategy.md)

---

> [!NOTE]
> **Status: Planned** — No CI/CD pipeline is currently implemented. This document is an engineering specification defining the target pipeline design. It will become the implementation guide and source of truth when the pipeline is built.

---

## Objective

Automate the build, test, and deployment of both Vetra repositories through a GitHub Actions pipeline that enforces quality gates, prevents regressions, and enables reliable, repeatable deployments to all environments.

---

## Pipeline Overview

```
Push / PR Event
       │
       ▼
┌──────────────────┐
│   CI Pipeline    │  ← Runs on every push and PR
│                  │
│  1. Checkout     │
│  2. Build        │
│  3. Lint/Style   │
│  4. Unit Tests   │
│  5. Integration  │
│     Tests        │
│  6. Security     │
│     Scan         │
└──────┬───────────┘
       │ All gates pass
       ▼
┌──────────────────┐
│   CD Pipeline    │  ← Runs on merge to main only
│                  │
│  7. Build Docker │
│     Image        │
│  8. Push to ECR  │
│  9. Deploy to    │
│     Environment  │
│  10. Smoke Test  │
└──────────────────┘
```

---

## Backend Pipeline — `omrajput14/vetra-backend`

**File:** `.github/workflows/backend-ci.yml`  
**Status:** Planned

### Triggers

```yaml
on:
  push:
    branches: [main, 'feature/**', 'fix/**']
  pull_request:
    branches: [main]
```

### CI Pipeline (All Branches)

```yaml
jobs:
  ci:
    name: Build, Lint, Test
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: vetra_test_db
          POSTGRES_USER: vetra_test_user
          POSTGRES_PASSWORD: vetra_test_pass
        ports:
          - 5432:5432
        options: --health-cmd pg_isready --health-retries 5

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build (skip tests)
        run: ./mvnw compile -B

      - name: Checkstyle Lint
        run: ./mvnw checkstyle:check -B

      - name: Run Unit Tests
        run: ./mvnw test -B

      - name: Run Integration Tests
        run: ./mvnw verify -B
        env:
          SPRING_PROFILES_ACTIVE: test
          DB_URL: jdbc:postgresql://localhost:5432/vetra_test_db
          DB_USERNAME: vetra_test_user
          DB_PASSWORD: vetra_test_pass
          JWT_SECRET: test-secret-minimum-32-chars-long

      - name: Upload Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: target/surefire-reports/
```

### Quality Gates

| Gate | Tool | Pass Condition |
|---|---|---|
| Compilation | Maven | Exit code 0 |
| Code style | Checkstyle | 0 violations |
| Unit tests | JUnit 5 + Mockito | 100% pass, 0 failures |
| Integration tests | Spring Boot Test + Testcontainers | 100% pass, 0 failures |
| Security scan | OWASP Dependency Check | No HIGH/CRITICAL CVEs |

### CD Pipeline (main branch only — Planned)

```yaml
  deploy-dev:
    name: Deploy to Development
    needs: ci
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker Image
        run: docker build -t $ECR_REGISTRY/vetra-backend:$GITHUB_SHA .

      - name: Push to ECR
        run: docker push $ECR_REGISTRY/vetra-backend:$GITHUB_SHA

      - name: Deploy to ECS (Dev)
        run: |
          aws ecs update-service \
            --cluster vetra-dev \
            --service vetra-backend \
            --force-new-deployment

      - name: Smoke Test
        run: |
          sleep 30
          curl -f https://dev.vetra.app/actuator/health
```

---

## Flutter Pipeline — `omrajput14/vetra`

**File:** `.github/workflows/flutter-ci.yml`  
**Status:** Planned

### CI Pipeline

```yaml
jobs:
  ci:
    name: Analyze, Test, Build
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.x'
          channel: 'stable'

      - name: Install dependencies
        run: flutter pub get

      - name: Verify no generated files tracked
        run: |
          git ls-files .dart_tool/ build/ *.iml .idea/ | wc -l | \
          xargs -I{} test {} -eq 0

      - name: Flutter Analyze
        run: flutter analyze --no-pub

      - name: Run Tests
        run: flutter test --coverage

      - name: Check Coverage Threshold
        run: |
          COVERAGE=$(lcov --summary coverage/lcov.info 2>&1 | grep "lines" | grep -o '[0-9.]*%' | head -1)
          echo "Coverage: $COVERAGE"
```

### Quality Gates

| Gate | Tool | Pass Condition |
|---|---|---|
| Analysis | `flutter analyze` | 0 errors, 0 warnings |
| Unit/Widget tests | `flutter test` | 100% pass |
| Code coverage | lcov | ≥ 70% line coverage |
| No generated files tracked | git ls-files | 0 generated files in index |

---

## Environment Promotion Strategy (Planned)

```
feature branch → PR → main → dev (auto) → staging (manual gate) → production (manual gate)
```

| Stage | Trigger | Approval Required |
|---|---|---|
| Dev | Merge to `main` | None — automatic |
| Staging | Tag `release/*` branch | Lead engineer |
| Production | Staging verified by QA | Lead engineer + one additional |

---

## Secret Management in CI

GitHub Actions secrets store all sensitive values. They are never logged or exposed in workflow outputs.

| Secret Name | Purpose |
|---|---|
| `DB_PASSWORD_DEV` | Dev database password |
| `JWT_SECRET_DEV` | Dev JWT signing key |
| `AWS_ACCESS_KEY_ID` | AWS deployment credentials |
| `AWS_SECRET_ACCESS_KEY` | AWS deployment credentials |
| `ECR_REGISTRY` | AWS ECR registry URL |

Secrets are injected as environment variables during the relevant job step only. They are scoped to the minimum required access.

---

## Rollback in CI/CD (Planned)

If a deployment fails smoke tests:

1. The pipeline automatically rolls back to the previous ECS task revision.
2. A GitHub issue is automatically created with the failed deployment log.
3. The `main` branch protection blocks further merges until the issue is resolved.
