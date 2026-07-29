# Git Workflow & Branching Strategy
**Document ID:** ENG-13  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** `omrajput14/vetra` (Flutter), `omrajput14/vetra-backend` (Spring Boot)  
**References:** [Engineering Principles](./00-principles.md)

---

## Overview

Vetra uses a **trunk-based development** workflow with short-lived feature branches. The `main` branch is always deployable. All development happens on feature branches that are merged via Pull Requests after review.

This document is authoritative for both the Flutter and Spring Boot repositories. Branch naming conventions, commit formats, and PR processes are identical across both repositories.

---

## Branch Strategy

### Branch Types

```
main
  └── feature/<scope>/<short-description>
  └── fix/<scope>/<short-description>
  └── chore/<short-description>
  └── docs/<short-description>
  └── hotfix/<short-description>
  └── release/<version>
  └── backup/<description>         ← rollback safety refs only
```

### Branch Rules

| Branch | Purpose | Merge Target | Lifetime |
|---|---|---|---|
| `main` | Production-ready code | — | Permanent |
| `feature/*` | New functionality | `main` via PR | Short (< 2 weeks) |
| `fix/*` | Bug fixes | `main` via PR | Short (< 3 days) |
| `chore/*` | Tooling, config, cleanup | `main` via PR | Short |
| `docs/*` | Documentation-only changes | `main` via PR | Short |
| `hotfix/*` | Critical production fix | `main` directly (emergency) | Hours |
| `release/*` | Release stabilization | `main` | Days |
| `backup/*` | Pre-destructive-operation safety refs | Never merged | Until confirmed safe |

### Branch Naming Examples

```bash
feature/auth/jwt-refresh-token-rotation
feature/medical-records/create-evmr-endpoint
fix/appointment/status-transition-validation
chore/update-checkstyle-rules
docs/add-api-versioning-guide
hotfix/security-patch-token-validation
release/v1.0.0
```

### Rules

1. **Always branch from latest `main`**: `git checkout main && git pull origin main && git checkout -b feature/...`
2. **One feature per branch**: Do not bundle unrelated changes into a single branch.
3. **Maximum branch lifetime**: 2 weeks. Long-lived branches accumulate merge conflicts and make review harder.
4. **No direct commits to `main`**: All changes must go through a Pull Request, except emergency hotfixes.
5. **Delete merged branches**: After a PR merges, the branch is deleted from remote.

---

## Commit Message Format

Vetra uses **Conventional Commits** (https://www.conventionalcommits.org).

### Format

```
<type>(<scope>): <short description>

[optional body]

[optional footer(s)]
```

### Types

| Type | When to Use |
|---|---|
| `feat` | A new feature or capability |
| `fix` | A bug fix |
| `docs` | Documentation changes only |
| `chore` | Build process, dependency updates, config changes with no production impact |
| `refactor` | Code restructuring with no behavior change |
| `test` | Adding or updating tests |
| `perf` | Performance improvements |
| `style` | Code formatting (no logic changes) |
| `revert` | Revert a previous commit |

### Scope (Backend Examples)

`auth`, `animal`, `appointment`, `medical-record`, `dashboard`, `db`, `security`, `docker`

### Scope (Flutter Examples)

`auth`, `animal`, `appointment`, `medical-record`, `farmer`, `vet`, `router`, `design-system`

### Rules

- **Imperative mood**: "Add endpoint" — not "Added endpoint" or "Adding endpoint"
- **Lowercase first letter** after the colon
- **Max 72 characters** for the subject line
- **Body explains why**, not what (the diff explains what)
- **Breaking changes** include `BREAKING CHANGE:` in the footer

### Good Examples

```
feat(medical-record): add GET /api/v1/medical-records/{id} endpoint

Implements veterinarian and farmer read access to EVMR records.
Authorization enforces that farmers can only access records
belonging to their own animals.

feat(auth): implement JWT refresh token rotation

fix(appointment): reject COMPLETED → PENDING status transition

Appointments in a terminal state must not be transitioned backward.
Fixes: #42

chore(docker): update PostgreSQL image to 15.6

docs(api): document medical-records endpoints in specification

BREAKING CHANGE: removed scheduled_at field from AppointmentResponse.
Use appointment_date + appointment_time instead.
```

### Bad Examples

```
fixed stuff                    ← No type, no scope, vague
Update files                   ← Vague, no type
WIP                            ← Commit WIP to a local branch, not remote
feat: added the new feature for users to be able to do things ← Too long
```

---

## Pull Request Process

### Before Opening a PR

- [ ] All tests pass locally
- [ ] `flutter analyze` (Flutter) or `./mvnw checkstyle:check` (backend) returns 0 errors
- [ ] No debug code, `print()` statements, or commented-out blocks
- [ ] Documentation has been updated in the same branch
- [ ] Commit history is clean (squash WIP commits)
- [ ] Self-reviewed the diff

### PR Title

Follow the same Conventional Commits format as commit messages:

```
feat(medical-record): implement Stage 7 EVMR backend module
fix(appointment): correct status validation for CANCELLED transitions
```

### PR Description Template

```markdown
## Summary
Brief description of what this PR does and why.

## Related Issue
Closes #<issue-number>

## Changes
- List of specific changes made

## Testing
- Unit tests added: Yes/No
- Integration tests: Yes/No
- Manually verified: Yes/No — describe verification steps

## Documentation Updated
- [ ] API specification
- [ ] Database design
- [ ] Domain model
- [ ] Other: ___

## Breaking Changes
None / Describe if any.
```

### Review Requirements

- Minimum **1 reviewer** for feature and fix PRs
- Minimum **2 reviewers** for security-sensitive changes, database migrations, or authentication changes
- All **blocking comments** must be resolved before merge
- PR author **must not self-merge**

### Merge Strategy

- **Squash and merge** for feature branches (clean, linear `main` history)
- **Merge commit** for release branches (preserves release point)
- **Rebase and merge** is discouraged (rewrites SHAs, complicates blame/bisect)

---

## Versioning & Tags

Vetra follows **Semantic Versioning** (https://semver.org): `MAJOR.MINOR.PATCH`

| Component | Meaning |
|---|---|
| `MAJOR` | Breaking change (API incompatibility, data migration required) |
| `MINOR` | New backward-compatible feature |
| `PATCH` | Backward-compatible bug fix |

### Tagging Releases

```bash
# Tag a release on main
git tag -a v1.0.0 -m "Release v1.0.0: Production launch — Stages 1-7 complete"
git push origin v1.0.0
```

### Current Version History

| Tag | Milestone |
|---|---|
| `stage-2-database` | Database schema foundation |
| `backup-before-cleanup` | Pre-repository-cleanup safety ref |
| `pre-filter-repo-backup` | Pre-history-rewrite safety ref |

---

## Handling Conflicts

1. Rebase your feature branch onto `main` (do not merge `main` into your branch):
   ```bash
   git fetch origin
   git rebase origin/main
   ```
2. Resolve conflicts file by file. For each conflict:
   - Understand both sides before choosing
   - Do not blindly accept either `ours` or `theirs`
3. After resolving: `git rebase --continue`
4. Force-push the rebased branch: `git push --force-with-lease origin feature/...`

---

## Emergency Hotfix Process

For critical production bugs (data corruption, security vulnerability, authentication bypass):

1. Create `hotfix/<description>` from `main`
2. Implement the minimal fix — no refactoring, no new features
3. Get emergency review (minimum 1 senior engineer)
4. Merge directly to `main`
5. Tag the hotfix: `git tag -a v1.0.1 -m "Hotfix: ..."`
6. Document in the Architecture Decision Log within 24 hours

---

## History Rewrite Policy

Rewriting published history (force-push to `main`) is prohibited except:

1. **Security emergency**: A secret or credential was committed. Requires immediate action.
2. **Pre-production only**: The push has not been consumed by any other developer.

All history rewrites must be documented in the Architecture Decision Log with:
- Date and reason
- Backup reference created before rewrite
- Engineers notified
