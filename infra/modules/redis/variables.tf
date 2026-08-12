variable "environment" {
  description = "Environment name (e.g., staging, production)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where Redis will be deployed"
  type        = string
}

variable "isolated_subnet_ids" {
  description = "List of isolated subnet IDs for the Redis subnet group"
  type        = list(string)
}

variable "sg_redis_id" {
  description = "Security Group ID for Redis"
  type        = string
}

variable "node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t4g.micro"
}

variable "num_cache_nodes" {
  description = "Number of cache nodes in the cluster"
  type        = number
  default     = 1
}

variable "engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.1" # Latest Redis 7 supported by AWS
}

variable "kms_key_id" {
  description = "KMS Key ID for encryption at rest (optional). If null, uses default AWS managed key."
  type        = string
  default     = null
}
