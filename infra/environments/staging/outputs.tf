# ─────────────────────────────────────────────────────────────────────────────
# Environment: staging — Outputs
# Exposes VPC outputs for downstream stages (ECS, RDS, ElastiCache).
# ─────────────────────────────────────────────────────────────────────────────

output "vpc_id" { value = module.vpc.vpc_id }
output "vpc_cidr" { value = module.vpc.vpc_cidr }
output "public_subnet_ids" { value = module.vpc.public_subnet_ids }
output "private_subnet_ids" { value = module.vpc.private_subnet_ids }
output "isolated_subnet_ids" { value = module.vpc.isolated_subnet_ids }
output "nat_public_ips" { value = module.vpc.nat_gateway_public_ips }
output "sg_alb_id" { value = module.vpc.sg_alb_id }
output "sg_ecs_id" { value = module.vpc.sg_ecs_id }
output "sg_rds_id" { value = module.vpc.sg_rds_id }
output "sg_redis_id" { value = module.vpc.sg_redis_id }
output "flow_log_group" { value = module.vpc.flow_log_group_name }

output "ecr_repository_name" { value = module.ecr.repository_name }
output "ecr_repository_url" {
  description = "URL of the ECR repository"
  value       = module.ecr.repository_url
}

# ── RDS PostgreSQL ────────────────────────────────────────────────────────────

output "rds_endpoint" {
  description = "The connection endpoint for the staging RDS instance"
  value       = module.rds.db_instance_endpoint
}

output "rds_password_secret_arn" {
  description = "The ARN of the Secrets Manager secret containing the staging DB password"
  value       = module.rds.db_password_secret_arn
}

# ── ElastiCache Redis ─────────────────────────────────────────────────────────

output "redis_primary_endpoint" {
  description = "The primary endpoint address for the staging Redis cluster"
  value       = module.redis.redis_primary_endpoint_address
}

output "redis_password_secret_arn" {
  description = "The ARN of the Secrets Manager secret containing the staging Redis AUTH token"
  value       = module.redis.redis_password_secret_arn
}
output "ecr_repository_arn" { value = module.ecr.repository_arn }
output "ecr_registry_id" { value = module.ecr.registry_id }

# ── IAM / OIDC ────────────────────────────────────────────────────────────────

output "github_actions_role_arn" {
  description = "ARN of the IAM role assumed by GitHub Actions via OIDC"
  value       = module.iam.github_actions_role_arn
}

output "github_actions_role_name" {
  description = "Name of the IAM role assumed by GitHub Actions via OIDC"
  value       = module.iam.github_actions_role_name
}

output "oidc_provider_arn" {
  description = "ARN of the GitHub Actions OIDC Identity Provider"
  value       = module.iam.oidc_provider_arn
}
