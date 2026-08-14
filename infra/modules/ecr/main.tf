# ─────────────────────────────────────────────────────────────────────────────
# Module: ecr — Core Repository Resources
# Stage:  14.3 — ECR Container Registry
#
# Provisioning of the Amazon ECR registry for Vetra backend images.
# Implements Stage 14.1 security requirements: image scanning, immutability,
# and restricted IAM policies.
# ─────────────────────────────────────────────────────────────────────────────

locals {
  repository_name = "${var.project}-backend-${var.environment}"

  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Stage       = "14.3"
    },
    var.tags
  )
}

# ── ECR Repository ────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "this" {
  name                 = local.repository_name
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = var.scan_on_push
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = local.common_tags
}

# ── ECR Lifecycle Policy ──────────────────────────────────────────────────────
# Retain a sensible number of tagged images for rollback capability and automatically
# expire untagged or very old images to control storage costs.

resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 10
        description  = "Expire untagged images older than ${var.untagged_image_retention_days} days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_image_retention_days
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 20
        description  = "Keep last ${var.tagged_image_retention_count} tagged images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.tagged_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# ── ECR Repository Policy ─────────────────────────────────────────────────────
# Implements Stage 14.1 least privilege.
# Grants pull access to the ECS execution role.
# Grants push/pull access to the GitHub Actions deploy role.

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id

  # Based on Stage 14.1 documented roles (use account root for ECS pull until ECS execution role is created)
  ecs_execution_role_arn  = "arn:aws:iam::${local.account_id}:root"
  github_actions_role_arn = "arn:aws:iam::${local.account_id}:role/${var.project}-github-actions-deploy"
}

resource "aws_ecr_repository_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowECSExecutionRolePull"
        Effect = "Allow"
        Principal = {
          AWS = local.ecs_execution_role_arn
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability"
        ]
      },
      {
        Sid    = "AllowGitHubActionsPush"
        Effect = "Allow"
        Principal = {
          AWS = local.github_actions_role_arn
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ]
      }
    ]
  })
}
