# ─────────────────────────────────────────────────────────────────────────────
# Module: ecs — Variables
# Stage:  14.7 — ECS Fargate & Application Load Balancer
# ─────────────────────────────────────────────────────────────────────────────

variable "project" {
  type        = string
  description = "Project name prefix (e.g. vetra)"
  default     = "vetra"
}

variable "environment" {
  type        = string
  description = "Deployment environment (e.g. staging, production)"
}

variable "aws_region" {
  type        = string
  description = "Target AWS region"
  default     = "ap-south-1"
}

variable "vpc_id" {
  type        = string
  description = "ID of the VPC where ECS and ALB reside"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs for ALB placement"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for ECS Fargate task placement"
}

variable "sg_alb_id" {
  type        = string
  description = "Security group ID for the Application Load Balancer"
}

variable "sg_ecs_id" {
  type        = string
  description = "Security group ID for ECS Fargate tasks"
}

variable "ecr_repository_url" {
  type        = string
  description = "URL of the ECR repository containing backend images"
}

variable "image_tag" {
  type        = string
  description = "Exact container image tag to deploy (e.g. Git commit SHA)"
}

variable "container_port" {
  type        = number
  description = "Application listening port inside container"
  default     = 8080
}

variable "cpu" {
  type        = number
  description = "Fargate CPU units (256, 512, 1024, etc.)"
  default     = 512
}

variable "memory" {
  type        = number
  description = "Fargate memory in MiB (512, 1024, 2048, etc.)"
  default     = 1024
}

variable "desired_count" {
  type        = number
  description = "Desired number of running task instances"
  default     = 1
}

variable "db_host" {
  type        = string
  description = "RDS PostgreSQL database host address"
}

variable "db_port" {
  type        = number
  description = "RDS PostgreSQL database port"
  default     = 5432
}

variable "db_name" {
  type        = string
  description = "Database name"
  default     = "vetra"
}

variable "db_user" {
  type        = string
  description = "Database master username"
  default     = "vetra"
}

variable "db_password_secret_arn" {
  type        = string
  description = "Secrets Manager ARN for RDS DB password"
}

variable "redis_host" {
  type        = string
  description = "ElastiCache Redis primary endpoint address"
}

variable "redis_port" {
  type        = number
  description = "ElastiCache Redis port"
  default     = 6379
}

variable "redis_password_secret_arn" {
  type        = string
  description = "Secrets Manager ARN for Redis AUTH token"
}

variable "tags" {
  type        = map(string)
  description = "Additional resource tags"
  default     = {}
}
