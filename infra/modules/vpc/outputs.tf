# ─────────────────────────────────────────────────────────────────────────────
# Module: vpc — Outputs
# Stage:  14.2 — VPC / Network Architecture
# ─────────────────────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "ID of the Vetra VPC."
  value       = aws_vpc.this.id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "IDs of public subnets (one per AZ). Used for ALB placement."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of private subnets (one per AZ). Used for ECS Fargate task placement."
  value       = aws_subnet.private[*].id
}

output "isolated_subnet_ids" {
  description = "IDs of isolated data subnets (one per AZ). Used for RDS and ElastiCache subnet groups."
  value       = aws_subnet.isolated[*].id
}

output "nat_gateway_ids" {
  description = "IDs of NAT Gateways. Empty list when enable_nat_gateway = false."
  value       = aws_nat_gateway.this[*].id
}

output "nat_gateway_public_ips" {
  description = "Elastic IP addresses of NAT Gateways. Add to external service allowlists if needed."
  value       = aws_eip.nat[*].public_ip
}

output "internet_gateway_id" {
  description = "ID of the Internet Gateway attached to the VPC."
  value       = aws_internet_gateway.this.id
}

# ── Security Group IDs ────────────────────────────────────────────────────────

output "sg_alb_id" {
  description = "Security group ID for the Application Load Balancer."
  value       = aws_security_group.alb.id
}

output "sg_ecs_id" {
  description = "Security group ID for ECS Fargate tasks."
  value       = aws_security_group.ecs.id
}

output "sg_rds_id" {
  description = "Security group ID for RDS PostgreSQL."
  value       = aws_security_group.rds.id
}

output "sg_redis_id" {
  description = "Security group ID for ElastiCache Redis."
  value       = aws_security_group.redis.id
}

# ── VPC Endpoint IDs ──────────────────────────────────────────────────────────

output "vpce_s3_id" {
  description = "ID of the S3 Gateway VPC endpoint."
  value       = aws_vpc_endpoint.s3.id
}

output "vpce_secretsmanager_id" {
  description = "ID of the Secrets Manager Interface VPC endpoint."
  value       = aws_vpc_endpoint.secretsmanager.id
}

output "vpce_ssm_id" {
  description = "ID of the SSM Parameter Store Interface VPC endpoint."
  value       = aws_vpc_endpoint.ssm.id
}

output "vpce_ecr_api_id" {
  description = "ID of the ECR API Interface VPC endpoint."
  value       = aws_vpc_endpoint.ecr_api.id
}

output "vpce_ecr_dkr_id" {
  description = "ID of the ECR DKR Interface VPC endpoint."
  value       = aws_vpc_endpoint.ecr_dkr.id
}

output "vpce_logs_id" {
  description = "ID of the CloudWatch Logs Interface VPC endpoint."
  value       = aws_vpc_endpoint.logs.id
}

# ── Flow Logs ─────────────────────────────────────────────────────────────────

output "flow_log_group_name" {
  description = "CloudWatch Log Group name for VPC Flow Logs. Empty string when flow logs disabled."
  value       = var.enable_flow_logs ? aws_cloudwatch_log_group.flow_logs[0].name : ""
}
