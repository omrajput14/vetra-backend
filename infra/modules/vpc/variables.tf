# ─────────────────────────────────────────────────────────────────────────────
# Module: vpc — Input Variables
# Stage:  14.2 — VPC / Network Architecture
# ─────────────────────────────────────────────────────────────────────────────

variable "environment" {
  type        = string
  description = "Deployment environment: staging or production."
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment must be 'staging' or 'production'."
  }
}

variable "project" {
  type        = string
  description = "Project name used in resource naming and tagging."
  default     = "vetra"
}

variable "aws_region" {
  type        = string
  description = "AWS region for all resources."
  default     = "ap-south-1"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the VPC. Must not overlap with other environments or on-premises ranges."
  # staging:    10.1.0.0/16  (65 534 host addresses)
  # production: 10.0.0.0/16  (65 534 host addresses)
  validation {
    condition     = can(cidrnetmask(var.vpc_cidr))
    error_message = "vpc_cidr must be a valid CIDR notation."
  }
}

variable "availability_zones" {
  type        = list(string)
  description = "List of Availability Zone names to use (must be within aws_region). ap-south-1 has three AZs."
  validation {
    condition     = length(var.availability_zones) >= 2
    error_message = "At least two availability zones are required for multi-AZ architecture."
  }
}

# ── Subnet CIDRs ─────────────────────────────────────────────────────────────
# Subnet sizing guide (all /24 = 251 usable IPs per subnet):
#   Public  (ALB):       2 subnets × /24
#   Private (ECS):       2 subnets × /24
#   Isolated (Data):     2 subnets × /24
# Remaining VPC space reserved for future expansion.

variable "public_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for public subnets (one per AZ). Hosts the ALB."
  validation {
    condition     = length(var.public_subnet_cidrs) >= 2
    error_message = "At least two public subnet CIDRs are required for multi-AZ ALB."
  }
}

variable "private_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for private subnets (one per AZ). Hosts ECS Fargate tasks."
  validation {
    condition     = length(var.private_subnet_cidrs) >= 2
    error_message = "At least two private subnet CIDRs are required for multi-AZ ECS."
  }
}

variable "isolated_subnet_cidrs" {
  type        = list(string)
  description = "CIDR blocks for isolated data subnets (one per AZ). Hosts RDS and ElastiCache. No internet route."
  validation {
    condition     = length(var.isolated_subnet_cidrs) >= 2
    error_message = "At least two isolated subnet CIDRs are required for multi-AZ data tier."
  }
}

variable "enable_nat_gateway" {
  type        = bool
  description = "When true, provisions a NAT Gateway so ECS tasks in private subnets can reach the internet (e.g., external AI APIs). Set to false for cost savings in staging if outbound internet is not required."
  default     = true
}

variable "single_nat_gateway" {
  type        = bool
  description = "When true, use a single NAT Gateway instead of one per AZ. Reduces cost but creates a single point of failure. Acceptable for staging; use false for production."
  default     = false
}

variable "enable_flow_logs" {
  type        = bool
  description = "Enable VPC Flow Logs (required by Stage 14.1 security baseline). Must be true in production."
  default     = true
}

variable "flow_log_retention_days" {
  type        = number
  description = "CloudWatch Logs retention in days for VPC Flow Logs."
  default     = 30
  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365], var.flow_log_retention_days)
    error_message = "flow_log_retention_days must be a valid CloudWatch retention value."
  }
}

variable "tags" {
  type        = map(string)
  description = "Additional tags to apply to all resources in this module."
  default     = {}
}
