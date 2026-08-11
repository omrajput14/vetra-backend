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
output "ecr_repository_url" { value = module.ecr.repository_url }
output "ecr_repository_arn" { value = module.ecr.repository_arn }
output "ecr_registry_id" { value = module.ecr.registry_id }
