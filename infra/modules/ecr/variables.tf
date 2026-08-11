# ─────────────────────────────────────────────────────────────────────────────
# Module: ecr — Input Variables
# Stage:  14.3 — ECR Container Registry
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

variable "image_tag_mutability" {
  type        = string
  description = "The tag mutability setting for the repository."
  default     = "IMMUTABLE"
}

variable "scan_on_push" {
  type        = bool
  description = "Indicates whether images are scanned after being pushed to the repository."
  default     = true
}

variable "untagged_image_retention_days" {
  type        = number
  description = "Number of days to keep untagged images before expiring them."
  default     = 7
}

variable "tagged_image_retention_count" {
  type        = number
  description = "Number of tagged images to retain. Older images will be expired."
  default     = 30
}

variable "tags" {
  type        = map(string)
  description = "Additional tags to apply to all resources in this module."
  default     = {}
}
