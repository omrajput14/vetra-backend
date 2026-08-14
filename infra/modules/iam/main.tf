# ─────────────────────────────────────────────────────────────────────────────
# Module: iam — GitHub Actions OIDC Provider & Deployment Role
# Stage:  14.1 / 14.4 — IAM / Security Foundation & CI Deployment
#
# Provisions the GitHub Actions OIDC identity provider and the IAM role
# required by GitHub Actions to publish container images to ECR.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Stage       = "14.1"
    },
    var.tags
  )

  role_name   = "${var.project}-github-actions-deploy"
  # Support standard "repo:owner/repo:ref:refs/heads/main" and GitHub ID-annotated "repo:owner@id/repo@id:ref:refs/heads/main"
  sub_pattern = "repo:${replace(var.github_repository, "/", "*/")}*:ref:${var.github_branch}"
}

# ── GitHub OIDC Identity Provider ──────────────────────────────────────────────
# One-time registration per AWS account. Creates the OIDC trust relationship
# with GitHub's token issuer.

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]

  # Official GitHub Actions OIDC root and intermediate CA thumbprints
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a2a851ce2c79274afe63393c248097610469"
  ]

  tags = local.common_tags
}

# ── GitHub Actions Deploy IAM Role ─────────────────────────────────────────────
# Assumed via sts:AssumeRoleWithWebIdentity by GitHub Actions workflows running
# on allowed branches in the specified repository.

resource "aws_iam_role" "github_actions_deploy" {
  name = local.role_name

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.github.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
          StringLike = {
            "token.actions.githubusercontent.com:sub" = local.sub_pattern
          }
        }
      }
    ]
  })

  tags = local.common_tags
}

# ── ECR Policy for GitHub Actions ─────────────────────────────────────────────
# Least-privilege policy granting ECR login and container publishing rights
# scoped strictly to the specified ECR repository.

resource "aws_iam_role_policy" "github_actions_ecr" {
  name = "${var.project}-github-actions-ecr-policy"
  role = aws_iam_role.github_actions_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "AllowECRAuthToken"
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        Sid    = "AllowECRPushPull"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage"
        ]
        Resource = var.ecr_repository_arn
      }
    ]
  })
}
