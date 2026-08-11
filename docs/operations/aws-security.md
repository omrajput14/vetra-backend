# AWS Security Architecture

**Document ID:** OPS-AWS-SEC-01
**Stage:** 14.1 — AWS IAM / Security Foundation
**Status:** Active — Authoritative
**Last Updated:** 2026-08-11
**Applies To:** `omrajput14/vetra-backend`
**References:**
- [Security Design](../security/11-security-design.md)
- [Environment Configuration](./24-environment-config.md)
- [CI/CD Pipeline](./15-cicd.md)
- [Deployment Architecture](../architecture/09-deployment.md)

---

> [!IMPORTANT]
> This document defines the AWS security baseline for all Vetra cloud deployments.
> No deviation from these principles is permitted without an explicit Architecture Decision Record (ADR).
> No production AWS resources are provisioned in Stage 14.1. This document governs Stage 14.2 onward.

---

## 1. AWS Environment Model

Vetra uses three distinct AWS environments. Each is a **fully isolated AWS account or at minimum an isolated set of IAM boundaries, S3 buckets, ECR repositories, and RDS instances**. Environments must never share secrets, credentials, or network access.

| Environment | AWS Account Strategy | Spring Profile | Purpose |
|---|---|---|---|
| `local` | No AWS account — Docker Compose only | `dev` | Individual developer machine |
| `staging` | Dedicated AWS account or isolated IAM | `staging` | Pre-production validation |
| `production` | Dedicated AWS account | `prod` | Live user traffic |

### Environment Isolation Principles

1. **Separate AWS accounts are preferred** over IAM-boundary-only separation for production.
2. A staging deployment failure must never affect production resources.
3. Production secrets must never be accessible from staging IAM roles.
4. Each environment has its own:
   - ECR repository (or repository with environment-tagged images)
   - RDS instance
   - ElastiCache cluster
   - S3 bucket
   - Secrets Manager namespace (`/vetra/staging/` vs `/vetra/prod/`)
   - CloudWatch log group

---

## 2. IAM Least-Privilege Architecture

### 2.1 Design Principles

- **Principle of Least Privilege**: Every IAM entity has only the permissions required for its specific function.
- **No root credentials**: AWS root account credentials are never used for application, deployment, or operational tasks.
- **No long-lived access keys for CI/CD**: GitHub Actions authenticates via OIDC (Section 3). Static `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` are prohibited for CI/CD.
- **No `AdministratorAccess` for runtime services**: ECS task roles, application roles, and CI/CD roles never carry `AdministratorAccess`.
- **Explicit deny > no allow**: Where a permission boundary must be enforced, use explicit deny.
- **Separate human and machine identities**: Human administrator access uses IAM users + MFA. Automated systems use IAM roles.

### 2.2 IAM Role Catalogue

#### Role 1: `vetra-github-actions-deploy`

**Purpose:** GitHub Actions OIDC deployment role. Used exclusively during CI/CD to push container images to ECR and update ECS services.

**Trust Policy Principal:** OIDC federation from `token.actions.githubusercontent.com`

**Conditions (must all be satisfied):**
- `sub` matches `repo:omrajput14/vetra-backend:ref:refs/heads/main` (production) or `refs/heads/staging` (staging)
- `aud` equals `sts.amazonaws.com`

**Permitted Actions (minimum viable):**
```
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:GetDownloadUrlForLayer
ecr:BatchGetImage
ecr:InitiateLayerUpload
ecr:UploadLayerPart
ecr:CompleteLayerUpload
ecr:PutImage
ecs:DescribeTaskDefinition
ecs:RegisterTaskDefinition
ecs:UpdateService
ecs:DescribeServices
ecs:DescribeClusters
iam:PassRole (scoped to vetra-ecs-execution-* only)
```

**Explicitly Denied:** Everything not listed above. No S3 access, no Secrets Manager write, no RDS access, no IAM modification.

---

#### Role 2: `vetra-ecs-execution-ENVIRONMENT`

**Purpose:** ECS task execution role. Assumed by the ECS agent (not the application) to pull container images and fetch secrets at task startup. One role per environment (`staging`, `prod`).

**Trust Policy Principal:** `ecs-tasks.amazonaws.com`

**Permitted Actions:**
```
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:GetDownloadUrlForLayer
ecr:BatchGetImage
logs:CreateLogStream
logs:PutLogEvents
secretsmanager:GetSecretValue
  Resource: arn:aws:secretsmanager:REGION:ACCOUNT:secret:/vetra/ENVIRONMENT/*
ssm:GetParameters
  Resource: arn:aws:ssm:REGION:ACCOUNT:parameter/vetra/ENVIRONMENT/*
```

**Explicitly Denied:** All S3 operations, all RDS direct operations, all IAM operations, all EC2 operations.

---

#### Role 3: `vetra-ecs-task-ENVIRONMENT`

**Purpose:** Application runtime role. Assumed by the running Vetra Spring Boot container to access AWS services during request processing. One role per environment.

**Trust Policy Principal:** `ecs-tasks.amazonaws.com`

**Permitted Actions:**
```
s3:PutObject
s3:GetObject
s3:DeleteObject
  Resource: arn:aws:s3:::vetra-ENVIRONMENT-media-storage/*
```

**Explicitly Denied:** `s3:DeleteBucket`, `s3:CreateBucket`, all IAM operations, all EC2 operations, all Secrets Manager write, all RDS direct access (application uses JDBC only).

**Future additions (as features require):**
- `ses:SendEmail` — scoped to verified SES identity only
- `sns:Publish` — scoped to specific topic ARN only

---

#### Role 4: `vetra-readonly-audit`

**Purpose:** Read-only operational access for security auditing, log review, and non-destructive investigation. Assigned to designated engineers for audit tasks only.

**Permitted Actions (AWS Managed Policy basis):**
- `ReadOnlyAccess` AWS managed policy (scoped to Vetra namespaces where possible)
- `CloudWatchLogsReadOnlyAccess`
- `AWSCloudTrailReadOnlyAccess`

**Prohibited:**
- No write to any resource
- No access to `secretsmanager:GetSecretValue`
- No access to `s3:GetObject` for production media without additional approval

---

#### Role 5: `vetra-breakglass-admin`

**Purpose:** Emergency administrative access. Used only during production incidents when normal operational procedures are insufficient.

**Trust Policy:** Requires MFA-authenticated IAM user assumption.

**Permitted Actions:** `AdministratorAccess` scoped with IAM condition `aws:MultiFactorAuthPresent = true`.

**Controls:**
- CloudTrail alert fires on every assumption of this role.
- Session duration maximum: 1 hour.
- Assumption requires a documented incident ticket reference (enforced by team process, not IAM).
- Usage is reviewed monthly.

---

### 2.3 Prohibited IAM Practices

| Prohibited Practice | Reason |
|---|---|
| Using root account credentials for any operation | Root cannot be restricted; compromise is catastrophic |
| Creating IAM users with static access keys for CI/CD | Keys can be leaked; OIDC eliminates this class of risk |
| Attaching `AdministratorAccess` to ECS task roles | Application runtime must not be able to modify infrastructure |
| Sharing IAM roles across environments | Staging compromise must not affect production |
| Inline policies with `"Resource": "*"` for destructive operations | Blast radius must be minimized |
| Storing AWS credentials in source code or GitHub Secrets for CI | Use OIDC; no static keys for GitHub Actions |

---

## 3. GitHub Actions OIDC Authentication

### 3.1 Architecture

GitHub Actions authenticates to AWS using OpenID Connect (OIDC) — not long-lived static access keys. This eliminates the entire class of secret rotation failures and key leak incidents for CI/CD.

```
omrajput14/vetra-backend (GitHub repository)
         |
         |  GitHub Actions workflow triggered by push to main
         v
GitHub Actions Runner
         |
         |  Requests OIDC token from GitHub
         v
GitHub OIDC Token (JWT)
  iss: token.actions.githubusercontent.com
  sub: repo:omrajput14/vetra-backend:ref:refs/heads/main
  aud: sts.amazonaws.com
         |
         |  aws-actions/configure-aws-credentials@v4
         |  calls sts:AssumeRoleWithWebIdentity
         v
AWS IAM Role: vetra-github-actions-deploy
  (Granted only if sub + aud conditions match)
         |
         +-- Push image to ECR
         +-- Update ECS service
```

### 3.2 OIDC Trust Policy Structure

The following trust policy must be applied to `vetra-github-actions-deploy` when the role is created in Stage 14.2+:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": [
            "repo:omrajput14/vetra-backend:ref:refs/heads/main",
            "repo:omrajput14/vetra-backend:environment:production"
          ]
        }
      }
    }
  ]
}
```

For staging deployments, a separate trust entry scopes to `refs/heads/staging` or `environment:staging`.

> [!CAUTION]
> Never use `StringLike` with a wildcard `*` in the `sub` condition in production trust policies.
> A wildcard allows any branch or PR to assume the role.
> Always use `StringEquals` with explicit branch/environment values.

### 3.3 GitHub Actions Workflow OIDC Usage Pattern

```yaml
permissions:
  id-token: write      # Required for OIDC token request
  contents: read

steps:
  - name: Configure AWS Credentials (OIDC)
    uses: aws-actions/configure-aws-credentials@v4
    with:
      role-to-assume: arn:aws:iam::ACCOUNT_ID:role/vetra-github-actions-deploy
      aws-region: ap-south-1
      role-session-name: vetra-github-actions-${{ github.run_id }}
```

### 3.4 OIDC IAM Identity Provider Setup

When the AWS account is configured in Stage 14.2, the following Identity Provider must be created (one-time, per AWS account):

| Field | Value |
|---|---|
| Provider URL | `https://token.actions.githubusercontent.com` |
| Audiences | `sts.amazonaws.com` |

---

## 4. Secret Management Strategy

### 4.1 Classification

| Classification | Storage | Examples |
|---|---|---|
| **Safe config** | `application.yml` / committed | Hikari pool size, log levels, CORS methods, S3 URL expiry minutes |
| **Environment variable (non-secret)** | Docker Compose env / ECS task definition env | `SPRING_PROFILES_ACTIVE`, `AWS_REGION`, `DB_HOST`, `REDIS_HOST`, `SERVER_PORT` |
| **AWS Secrets Manager** | `/vetra/ENVIRONMENT/SECRET_NAME` | DB password, JWT secret, Gemini API key, Firebase private key, Redis password |
| **AWS SSM Parameter Store (SecureString)** | `/vetra/ENVIRONMENT/PARAM_NAME` | Non-critical but environment-specific string values |
| **GitHub Actions OIDC** | GitHub repository OIDC settings | AWS role ARN (not a secret), AWS region |

> [!IMPORTANT]
> `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must **never** be stored as GitHub Actions secrets for CI/CD.
> OIDC eliminates the need for these entirely.

### 4.2 AWS Secrets Manager Namespace

```
/vetra/staging/
  db/password
  jwt/secret
  redis/password
  gemini/api-key
  firebase/project-id
  firebase/client-email
  firebase/private-key

/vetra/prod/
  db/password
  jwt/secret
  redis/password
  gemini/api-key
  firebase/project-id
  firebase/client-email
  firebase/private-key
```

The ECS execution role has `secretsmanager:GetSecretValue` scoped **only** to `/vetra/ENVIRONMENT/*`.
The production role cannot read staging secrets and vice versa.

### 4.3 Secret Injection into ECS

Secrets are injected at ECS task definition time as ARN references — the ECS agent fetches values before the container process begins. No plaintext secrets appear in task definition JSON.

```json
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:ap-south-1:ACCOUNT_ID:secret:/vetra/prod/db/password"
    },
    {
      "name": "JWT_SECRET",
      "valueFrom": "arn:aws:secretsmanager:ap-south-1:ACCOUNT_ID:secret:/vetra/prod/jwt/secret"
    },
    {
      "name": "REDIS_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:ap-south-1:ACCOUNT_ID:secret:/vetra/prod/redis/password"
    },
    {
      "name": "GEMINI_API_KEY",
      "valueFrom": "arn:aws:secretsmanager:ap-south-1:ACCOUNT_ID:secret:/vetra/prod/gemini/api-key"
    }
  ]
}
```

The Spring Boot application reads these as standard environment variables — no application code change is required.

### 4.4 Secret Inventory (Names Only — No Values)

| Secret Name | Sensitivity | Current Storage | Target Storage |
|---|---|---|---|
| `DB_PASSWORD` / `POSTGRES_PASSWORD` | Critical | `.env` (local only, gitignored) | AWS Secrets Manager |
| `JWT_SECRET` | Critical | `.env` (local only, gitignored) | AWS Secrets Manager |
| `REDIS_PASSWORD` | High | `.env` (local only, gitignored) | AWS Secrets Manager |
| `GEMINI_API_KEY` | High | `.env` (local only, gitignored) | AWS Secrets Manager |
| `FCM_PRIVATE_KEY` | High | `.env` (local only, gitignored) | AWS Secrets Manager |
| `FCM_PROJECT_ID` | Config | `.env.example` (placeholder only) | AWS Secrets Manager or ECS env |
| `FCM_CLIENT_EMAIL` | Config | `.env.example` (placeholder only) | AWS Secrets Manager |
| `AWS_REGION` | Safe config | `.env.example`, `application.yml` | ECS task definition env (non-secret) |
| `AWS_S3_BUCKET` | Safe config | `.env.example`, `application.yml` | ECS task definition env (non-secret) |
| `CORS_ALLOWED_ORIGINS` | Safe config | `application-prod.yml` | ECS task definition env (non-secret) |

### 4.5 Credential Rotation Schedule

| Secret | Rotation Frequency | Mechanism |
|---|---|---|
| `DB_PASSWORD` | 90 days | AWS Secrets Manager automatic rotation (Lambda rotator) |
| `JWT_SECRET` | 180 days or on security incident | Manual rotation via Secrets Manager, requires rolling restart |
| `REDIS_PASSWORD` | 90 days | Manual rotation coordinated with ElastiCache AUTH token update |
| `GEMINI_API_KEY` | Per vendor recommendation or on leak | Manual rotation via Gemini console |
| `FCM_PRIVATE_KEY` | Per vendor recommendation or on leak | New service account key via Firebase console |

---

## 5. Root Account Protection

| Control | Requirement |
|---|---|
| MFA on root account | Mandatory — hardware MFA key preferred |
| Root account email | Dedicated group email (not individual), stored in secure vault |
| Root account usage | Zero routine usage — root is only for account recovery and billing |
| Root API access keys | Must not exist — delete immediately if found |
| CloudTrail alert on root login | Mandatory — SNS alert fires immediately |
| AWS Organizations | Recommended — enables Service Control Policies across child accounts |

---

## 6. Audit Logging

### 6.1 AWS CloudTrail Requirements

| Setting | Requirement |
|---|---|
| Multi-region trail | Enabled — catches global services (IAM, STS) |
| S3 log storage | Dedicated S3 bucket with versioning and MFA-delete |
| Log file integrity validation | Enabled |
| CloudWatch Logs integration | Enabled — enables metric filter alarms |
| Retention | Minimum 1 year in S3 |

### 6.2 Required CloudWatch Alarms on CloudTrail

| Alert | Trigger |
|---|---|
| Root account login | `$.userIdentity.type = "Root"` |
| `vetra-breakglass-admin` role assumption | `sts:AssumeRole` with target role ARN |
| IAM policy change | `PutRolePolicy`, `AttachRolePolicy`, `CreatePolicy` |
| Security group modification | `AuthorizeSecurityGroupIngress`, `RevokeSecurityGroupIngress` |
| Secrets Manager access from unexpected principal | Custom metric filter |
| Failed authentication burst (>5 in 5 minutes) | `errorCode = "AccessDenied"` aggregation |

### 6.3 Application Logging to CloudWatch

```
Log Group:  /vetra/ENVIRONMENT/application
Log Stream: ecs/vetra-backend/{task-id}
Retention:  30 days (staging), 90 days (production)
```

---

## 7. Encryption

### 7.1 Data at Rest

| Resource | Encryption | Key |
|---|---|---|
| RDS PostgreSQL | Enabled | AWS-managed KMS key |
| ElastiCache Redis | Enabled | AWS-managed key |
| S3 (media bucket) | SSE-S3 or SSE-KMS | AWS-managed or CMK |
| Secrets Manager | Default encryption | AWS-managed KMS key |
| CloudTrail S3 logs | SSE-S3 | AWS-managed |
| EBS volumes (ECS hosts, if EC2 launch type) | Encrypted | AWS-managed key |

### 7.2 Data in Transit

| Connection | Encryption |
|---|---|
| HTTPS to ALB | TLS 1.2+ enforced, TLS 1.0/1.1 disabled |
| ALB to ECS (internal) | TLS optional — acceptable within private VPC subnet |
| ECS to RDS | SSL required (`?sslmode=require` in JDBC URL) |
| ECS to ElastiCache Redis | TLS in-transit encryption enabled |
| ECS to Secrets Manager/SSM | HTTPS (always, enforced by AWS SDK) |
| ECS to S3 | HTTPS (always, enforced by AWS SDK) |

---

## 8. Network Security Requirements

*(Full VPC design is implemented in Stage 14.2. The following are binding requirements.)*

- ECS tasks run in **private subnets** — no direct internet route.
- ALB runs in **public subnets** — sole internet entry point for HTTP/HTTPS.
- RDS and ElastiCache run in **isolated subnets** — no internet route, no public IP.
- Security groups follow least-privilege ingress:
  - ALB SG: Port 443 inbound from `0.0.0.0/0`; port 80 redirect only
  - ECS SG: Port 8080 inbound from ALB SG only
  - RDS SG: Port 5432 inbound from ECS SG only
  - ElastiCache SG: Port 6379 inbound from ECS SG only
- No SSH / direct instance access — use ECS Exec (logged via CloudTrail) for emergency shell access.
- VPC Flow Logs enabled for security analysis.
- S3 VPC Endpoint — S3 traffic stays within AWS network.
- Secrets Manager VPC Endpoint — secret fetches stay within VPC.

---

## 9. Incident Response and Break-Glass Procedure

### 9.1 Break-Glass Access

1. Create an incident ticket with justification.
2. A second engineer must approve the break-glass assumption.
3. Assume `vetra-breakglass-admin` role with MFA-authenticated IAM user.
4. CloudTrail alert fires automatically — incident channel receives notification.
5. All actions under this role are CloudTrail-logged.
6. Revoke any temporary resources created under this role upon incident resolution.
7. Conduct post-incident review within 48 hours.

### 9.2 Credential Compromise Response

1. **Immediately invalidate** the credential (deactivate access key or deny role sessions).
2. **Rotate** the affected secret in Secrets Manager and trigger ECS rolling restart.
3. **Audit CloudTrail** for actions taken using the compromised credential in the preceding 30 days.
4. **Notify** affected users if any unauthorized data access is confirmed.
5. **Post-incident review** within 72 hours.

> [!CAUTION]
> Do NOT rotate credentials by creating new ones alongside old ones.
> The compromised credential must be **deactivated first**, then replaced.

---

## 10. Prohibited Practices

The following are explicitly prohibited in all Vetra AWS environments:

1. Storing `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` in GitHub Actions secrets for CI/CD (use OIDC).
2. Using root account credentials for any routine operation.
3. Creating ECS task roles with `AdministratorAccess` or `PowerUserAccess`.
4. Committing any secret, credential, or API key to source control (enforced by Gitleaks).
5. Disabling CloudTrail in any Vetra AWS account.
6. Creating S3 buckets with public-read ACLs (all buckets must be private; presigned URLs for client access).
7. Creating security group rules with `0.0.0.0/0` on any port except 80/443 on the ALB.
8. Using `StringLike` with wildcard in OIDC trust policy `sub` condition in production.
9. Sharing IAM roles across staging and production environments.
10. Deploying application containers as root user (Vetra Dockerfile already enforces non-root `vetra` user).

---

## 11. AWS Region Strategy

**Primary region:** `ap-south-1` (Mumbai)

**Justification:** Vetra serves livestock and veterinary healthcare in South Asia. `ap-south-1` minimizes latency for target users and aligns with existing `application.yml` defaults.

**Multi-region:** Not required at Stage 14.x. Re-evaluate when SLA requires >99.9% availability or regulatory data-residency requirements emerge.

---

## 12. Repository Security Audit Findings (Stage 14.1)

The following findings were identified during the Stage 14.1 repository audit.

### 12.1 Security Strengths (No Action Required)

| Finding | Status |
|---|---|
| `.env` file is gitignored | Confirmed — `.gitignore` includes `.env` and `*.env` |
| `.env` is not git-tracked | Confirmed — `git ls-files` shows only `.env.example` |
| No AWS access key IDs in tracked files | Confirmed — git grep found none |
| No private keys in tracked files | Confirmed — grep found none |
| Dockerfile uses non-root user | Confirmed — `USER vetra` enforced in production stage |
| Gitleaks secret scanner active in CI | Confirmed — `.github/workflows/ci.yml` job 3 |
| Docker Compose environment values use env-var substitution only | Confirmed — `${VAR:-default}` pattern, no hardcoded secrets |
| `.dockerignore` excludes `.env.*` files | Confirmed |
| GitHub Actions `permissions: contents: read` at top level | Confirmed — minimal by default |
| Dependabot enabled for Maven and GitHub Actions | Confirmed |

### 12.2 Current Security Gaps (To Be Addressed in Stage 14.2+)

| Gap | Severity | Remediation Stage |
|---|---|---|
| No AWS OIDC identity provider configured | High | Stage 14.2 |
| IAM roles not yet created | High | Stage 14.2 |
| Secrets not yet in AWS Secrets Manager (rely on local `.env`) | High | Stage 14.3 (ECS) |
| No CloudTrail enabled | High | Stage 14.2 |
| No VPC / network isolation | High | Stage 14.2 |
| `application-prod.yml` has default fallback values for DB/Redis passwords | Medium | Stage 14.3 — ECS task definition supersedes defaults |
| `docker-compose.yml` has default fallback values for secrets | Low | Acceptable for local dev; never used in production |
| GitHub Actions CI does not yet have `id-token: write` permission | Low | Stage 14.4 (CD workflow) |

---

## 13. Prerequisites for Stage 14.2

Stage 14.1 is complete. Stage 14.2 (VPC / Network Architecture) may begin when:

1. An AWS account has been created or designated for Vetra (staging and/or production).
2. AWS root account MFA has been enabled and root access keys confirmed absent.
3. CloudTrail has been enabled with multi-region trail and S3 + CloudWatch Logs sink.
4. The GitHub OIDC identity provider has been registered in the AWS account (`token.actions.githubusercontent.com`, audience `sts.amazonaws.com`).
5. The five IAM roles defined in Section 2.2 have been created with the policies documented above.
6. Secrets Manager namespaces `/vetra/staging/` and `/vetra/prod/` have been bootstrapped with placeholder secret structures (real values populated before first ECS deployment).
7. This document has been reviewed by the team.

---

*This document is authoritative for Vetra AWS security. All future AWS deployment stages must comply with the principles defined here.*
