# ─────────────────────────────────────────────────────────────────────────────
# Environment: production
# Stage:  14.2 — VPC / Network Architecture
# Region: ap-south-1 (Mumbai)
#
# CIDR design (production, 10.0.0.0/16):
#
#   VPC:                    10.0.0.0/16
#
#   Public subnets (ALB):
#     ap-south-1a:          10.0.1.0/24
#     ap-south-1b:          10.0.2.0/24
#     ap-south-1c:          10.0.3.0/24  (third AZ for production HA)
#
#   Private subnets (ECS):
#     ap-south-1a:          10.0.10.0/24
#     ap-south-1b:          10.0.11.0/24
#     ap-south-1c:          10.0.12.0/24
#
#   Isolated subnets (Data):
#     ap-south-1a:          10.0.20.0/24
#     ap-south-1b:          10.0.21.0/24
#     ap-south-1c:          10.0.22.0/24
#
#   Reserved for future expansion: 10.0.30.0/24 onward
#
# Production uses three AZs and one NAT Gateway per AZ for full HA.
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "vetra-terraform-state-ACCOUNT_ID"
    key            = "production/vpc/terraform.tfstate"
    region         = "ap-south-1"
    encrypt        = true
    dynamodb_table = "vetra-terraform-locks"
  }
}

provider "aws" {
  region = "ap-south-1"

  default_tags {
    tags = {
      Project     = "vetra"
      Environment = "production"
      ManagedBy   = "terraform"
      Stage       = "14.2"
    }
  }
}

module "vpc" {
  source = "../../modules/vpc"

  environment = "production"
  project     = "vetra"
  aws_region  = "ap-south-1"

  vpc_cidr = "10.0.0.0/16"

  availability_zones = [
    "ap-south-1a",
    "ap-south-1b",
    "ap-south-1c"
  ]

  public_subnet_cidrs = [
    "10.0.1.0/24", # ap-south-1a
    "10.0.2.0/24", # ap-south-1b
    "10.0.3.0/24", # ap-south-1c
  ]

  private_subnet_cidrs = [
    "10.0.10.0/24", # ap-south-1a
    "10.0.11.0/24", # ap-south-1b
    "10.0.12.0/24", # ap-south-1c
  ]

  isolated_subnet_cidrs = [
    "10.0.20.0/24", # ap-south-1a
    "10.0.21.0/24", # ap-south-1b
    "10.0.22.0/24", # ap-south-1c
  ]

  # Production: one NAT Gateway per AZ for full high availability.
  # If one AZ fails, ECS tasks in other AZs retain outbound internet access.
  enable_nat_gateway = true
  single_nat_gateway = false # One NAT per AZ — required for production HA

  enable_flow_logs        = true
  flow_log_retention_days = 30
}

# ── ECR Container Registry ────────────────────────────────────────────────────

module "ecr" {
  source = "../../modules/ecr"

  environment = "production"
  project     = "vetra"

  # Keep untagged images for 7 days in production
  untagged_image_retention_days = 7

  # Keep 30 most recent tagged production images for rollback capability
  tagged_image_retention_count = 30
}
