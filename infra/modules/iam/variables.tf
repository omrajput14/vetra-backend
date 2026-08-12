variable "project" {
  type        = string
  description = "Project name prefix"
  default     = "vetra"
}

variable "environment" {
  type        = string
  description = "Deployment environment (e.g. staging, prod)"
  default     = "staging"
}

variable "github_repository" {
  type        = string
  description = "GitHub repository in owner/repo format"
  default     = "omrajput14/vetra-backend"
}

variable "github_branch" {
  type        = string
  description = "Target Git ref branch allowed to assume the deploy role"
  default     = "refs/heads/main"
}

variable "ecr_repository_arn" {
  type        = string
  description = "ARN of the ECR repository the role is granted permission to push images to"
}

variable "tags" {
  type        = map(string)
  description = "Additional tags to merge with common tags"
  default     = {}
}
