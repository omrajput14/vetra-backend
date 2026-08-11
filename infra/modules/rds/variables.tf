variable "environment" {
  description = "Environment name (e.g., staging, production)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where RDS will be deployed"
  type        = string
}

variable "isolated_subnet_ids" {
  description = "List of isolated subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "sg_rds_id" {
  description = "Security Group ID for RDS"
  type        = string
}

variable "instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 20
}

variable "multi_az" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = false
}

variable "engine_version" {
  description = "PostgreSQL engine version"
  type        = string
  default     = "15.7"
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "vetra"
}

variable "db_user" {
  description = "Master database username"
  type        = string
  default     = "vetra"
}

variable "kms_key_id" {
  description = "KMS Key ID for encryption at rest (optional). If null, uses default AWS managed key."
  type        = string
  default     = null
}
