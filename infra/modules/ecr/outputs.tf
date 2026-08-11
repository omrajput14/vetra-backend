# ─────────────────────────────────────────────────────────────────────────────
# Module: ecr — Outputs
# Stage:  14.3 — ECR Container Registry
# ─────────────────────────────────────────────────────────────────────────────

output "repository_name" {
  description = "The name of the ECR repository."
  value       = aws_ecr_repository.this.name
}

output "repository_url" {
  description = "The URL of the ECR repository."
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "The ARN of the ECR repository."
  value       = aws_ecr_repository.this.arn
}

output "registry_id" {
  description = "The registry ID where the repository was created."
  value       = aws_ecr_repository.this.registry_id
}
