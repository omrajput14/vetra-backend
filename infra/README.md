# Vetra Infrastructure — Terraform

**Stage 14.2 — VPC / Network Architecture**

This directory contains the Terraform infrastructure-as-code for the Vetra backend AWS deployment.

## Directory Structure

```
infra/
├── README.md                          ← This file
├── modules/
│   ├── vpc/                           ← Reusable VPC module
│   │   ├── main.tf                    ← VPC, subnets, IGW, NAT, route tables, VPC endpoints, flow logs
│   │   ├── security_groups.tf         ← ALB, ECS, RDS, Redis security groups
│   │   ├── variables.tf               ← All module input variables
│   │   └── outputs.tf                 ← All module outputs for downstream stages
│   ├── ecr/                           ← Reusable ECR module
│   │   ├── main.tf                    ← ECR Repository, Lifecycle Policy, Repository Policy
│   │   ├── variables.tf               ← Module input variables
│   │   └── outputs.tf                 ← Module outputs
│   └── iam/                           ← Reusable IAM / OIDC module
│       ├── main.tf                    ← GitHub Actions OIDC Provider & Deploy Role
│       ├── variables.tf               ← Module input variables
│       └── outputs.tf                 ← Module outputs
└── environments/
    ├── staging/                       ← Staging environment configuration
    │   ├── main.tf                    ← CIDR: 10.1.0.0/16, single NAT, 2 AZs, ECR repository
    │   └── outputs.tf
    └── production/                    ← Production environment configuration
        ├── main.tf                    ← CIDR: 10.0.0.0/16, per-AZ NAT, 3 AZs, ECR repository
        └── outputs.tf
```

## Container Registry (ECR)

Each environment runs its own isolated Amazon ECR registry (`vetra-backend-staging` and `vetra-backend-production`) to store container images for ECS Fargate.

- **Naming Convention:** `vetra-backend-ENVIRONMENT`
- **Tagging Strategy & Immutability:** Image tags are `IMMUTABLE`. `latest` should not be used as the deployment identity. Images must be tagged with a unique identifier (e.g., Git commit SHA or release version).
- **Security & Scanning:** "Scan on push" is enabled. Images are automatically scanned for vulnerabilities using Clair/Inspector upon upload. ECR data is encrypted at rest (AES-256).
- **Lifecycle Policy (Cost Control & Rollback):**
  - **Untagged Images:** Automatically expired after a defined period (3 days staging, 7 days production) to manage costs.
  - **Tagged Images:** Retains a sensible number of recent images (10 staging, 30 production) to ensure rollback capability without unbounded storage growth.
- **Access Control:** Follows the Stage 14.1 least-privilege model. Only the GitHub Actions CI/CD role can push images. Only the designated ECS Execution role can pull images. Staging and production IAM roles are strictly separated.


## Security Baseline

All network configuration satisfies the requirements in
[docs/operations/aws-security.md](../docs/operations/aws-security.md) (Stage 14.1).

Key controls:
- ECS tasks in **private subnets** — no public IP
- ALB in **public subnets** — sole internet entry point
- RDS and ElastiCache in **isolated subnets** — no internet route
- Least-privilege security groups (ALB → ECS → RDS/Redis chain only)
- No SSH on any security group
- VPC Flow Logs enabled
- S3 Gateway VPC endpoint (free, all traffic stays in AWS network)
- Interface VPC endpoints: Secrets Manager, SSM, ECR API, ECR DKR, CloudWatch Logs

## CIDR Allocation

| Environment | VPC CIDR     | Notes |
|---|---|---|
| staging     | 10.1.0.0/16  | 2 AZs, single NAT for cost savings |
| production  | 10.0.0.0/16  | 3 AZs, per-AZ NAT for HA |

These ranges do not overlap and are reserved exclusively for Vetra.

## Prerequisites Before `terraform apply`

1. AWS account created and configured (see Stage 14.1 prerequisites)
2. Terraform >= 1.5.0 installed
3. AWS CLI configured with appropriate credentials for the target environment
4. Remote state S3 bucket and DynamoDB locks table created (one-time bootstrap):
   ```bash
   # Bootstrap remote state (run once per AWS account)
   aws s3 mb s3://vetra-terraform-state-ACCOUNT_ID --region ap-south-1
   aws s3api put-bucket-versioning \
     --bucket vetra-terraform-state-ACCOUNT_ID \
     --versioning-configuration Status=Enabled
   aws s3api put-bucket-encryption \
     --bucket vetra-terraform-state-ACCOUNT_ID \
     --server-side-encryption-configuration \
     '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
   aws dynamodb create-table \
     --table-name vetra-terraform-locks \
     --attribute-definitions AttributeName=LockID,AttributeType=S \
     --key-schema AttributeName=LockID,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --region ap-south-1
   ```
5. Update `ACCOUNT_ID` in `backend "s3"` blocks with the real AWS account ID (do NOT commit real IDs)

## Applying Infrastructure

```bash
# Staging
cd infra/environments/staging
terraform init
terraform plan -out=tfplan
terraform apply tfplan

# Production (requires explicit confirmation)
cd infra/environments/production
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

## Secrets

No secrets, API keys, or account IDs are stored in this directory.
All sensitive values are managed in AWS Secrets Manager under `/vetra/ENVIRONMENT/`.
See [docs/operations/aws-security.md §4](../docs/operations/aws-security.md) for the complete secret inventory.
