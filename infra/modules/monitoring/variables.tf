# ─────────────────────────────────────────────────────────────────────────────
# Module: monitoring — Variables
# Stage:  14.10 — Observability & Monitoring
# ─────────────────────────────────────────────────────────────────────────────

variable "project" {
  type        = string
  description = "Project name identifier"
  default     = "vetra"
}

variable "environment" {
  type        = string
  description = "Deployment environment (e.g. staging, prod)"
}

variable "aws_region" {
  type        = string
  description = "AWS deployment region"
  default     = "ap-south-1"
}

variable "tags" {
  type        = map(string)
  description = "Resource tags"
  default     = {}
}

# ── ECS Target Identifiers ───────────────────────────────────────────────────

variable "ecs_cluster_name" {
  type        = string
  description = "Name of the ECS Cluster to monitor"
}

variable "ecs_service_name" {
  type        = string
  description = "Name of the ECS Service to monitor"
}

# ── ALB Target Identifiers ───────────────────────────────────────────────────

variable "alb_arn_suffix" {
  type        = string
  description = "ARN suffix of the Application Load Balancer for CloudWatch metrics"
}

variable "target_group_arn_suffix" {
  type        = string
  description = "ARN suffix of the ALB Target Group for CloudWatch metrics"
}

# ── Data Tier Identifiers ─────────────────────────────────────────────────────

variable "db_instance_id" {
  type        = string
  description = "Identifier of the RDS PostgreSQL instance to monitor"
}

variable "redis_replication_group_id" {
  type        = string
  description = "Replication Group ID of the ElastiCache Redis cluster to monitor"
}

# ── CloudWatch Logs ───────────────────────────────────────────────────────────

variable "ecs_log_group_name" {
  type        = string
  description = "Name of the CloudWatch Log Group for ECS container logs"
}

# ── Notification Configuration ────────────────────────────────────────────────

variable "alert_email" {
  type        = string
  description = "Optional email address to subscribe to CloudWatch alarm notifications via SNS"
  default     = ""
}
