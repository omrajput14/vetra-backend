# ─────────────────────────────────────────────────────────────────────────────
# Environment: staging
# Stage:  14.2 — VPC / Network Architecture
# Region: ap-south-1 (Mumbai)
#
# CIDR design (staging, 10.1.0.0/16):
#
#   VPC:                    10.1.0.0/16
#
#   Public subnets (ALB):
#     ap-south-1a:          10.1.1.0/24
#     ap-south-1b:          10.1.2.0/24
#
#   Private subnets (ECS):
#     ap-south-1a:          10.1.10.0/24
#     ap-south-1b:          10.1.11.0/24
#
#   Isolated subnets (Data):
#     ap-south-1a:          10.1.20.0/24
#     ap-south-1b:          10.1.21.0/24
#
#   Reserved for future expansion: 10.1.30.0/24 onward
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state: configure before first apply.
  # Replace ACCOUNT_ID and bucket name with actual values.
  # Do NOT commit a populated backend config with real account IDs.
  backend "s3" {
    bucket         = "vetra-terraform-state-ACCOUNT_ID"
    key            = "staging/vpc/terraform.tfstate"
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
      Environment = "staging"
      ManagedBy   = "terraform"
      Stage       = "14.2"
    }
  }
}

module "vpc" {
  source = "../../modules/vpc"

  environment = "staging"
  project     = "vetra"
  aws_region  = "ap-south-1"

  vpc_cidr = "10.1.0.0/16"

  availability_zones = [
    "ap-south-1a",
    "ap-south-1b"
  ]

  public_subnet_cidrs = [
    "10.1.1.0/24", # ap-south-1a
    "10.1.2.0/24", # ap-south-1b
  ]

  private_subnet_cidrs = [
    "10.1.10.0/24", # ap-south-1a
    "10.1.11.0/24", # ap-south-1b
  ]

  isolated_subnet_cidrs = [
    "10.1.20.0/24", # ap-south-1a
    "10.1.21.0/24", # ap-south-1b
  ]

  # Staging: single NAT gateway to minimize cost.
  # This creates a single point of failure for outbound internet — acceptable
  # for staging but NOT for production.
  enable_nat_gateway = true
  single_nat_gateway = true

  enable_flow_logs        = true
  flow_log_retention_days = 14 # Shorter retention for staging cost efficiency
}

# ── ECR Container Registry ────────────────────────────────────────────────────

module "ecr" {
  source = "../../modules/ecr"

  environment = "staging"
  project     = "vetra"

  # Keep untagged images for 3 days to control costs in staging
  untagged_image_retention_days = 3

  # Keep 10 most recent tagged staging images
  tagged_image_retention_count = 10
}

# ── RDS PostgreSQL ────────────────────────────────────────────────────────────

module "rds" {
  source = "../../modules/rds"

  environment = "staging"

  vpc_id              = module.vpc.vpc_id
  isolated_subnet_ids = module.vpc.isolated_subnet_ids
  sg_rds_id           = module.vpc.sg_rds_id

  instance_class    = "db.t4g.micro"
  allocated_storage = 20
  multi_az          = false
  engine_version    = "15.13"
}

# ── ElastiCache Redis ─────────────────────────────────────────────────────────

module "redis" {
  source = "../../modules/redis"

  environment = "staging"

  vpc_id              = module.vpc.vpc_id
  isolated_subnet_ids = module.vpc.isolated_subnet_ids
  sg_redis_id         = module.vpc.sg_redis_id

  node_type       = "cache.t4g.micro"
  num_cache_nodes = 1
}

# ── IAM / OIDC GitHub Actions Deployment Role ─────────────────────────────────

module "iam" {
  source = "../../modules/iam"

  environment        = "staging"
  project            = "vetra"
  github_repository  = "omrajput14/vetra-backend"
  github_branch      = "refs/heads/main"
  ecr_repository_arn = module.ecr.repository_arn
}

# ── ECS Fargate & Application Load Balancer ───────────────────────────────────

module "ecs" {
  source = "../../modules/ecs"

  environment = "staging"
  project     = "vetra"
  aws_region  = "ap-south-1"

  vpc_id             = module.vpc.vpc_id
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids
  sg_alb_id          = module.vpc.sg_alb_id
  sg_ecs_id          = module.vpc.sg_ecs_id

  ecr_repository_url = module.ecr.repository_url
  image_tag          = "6ba5a7f2512e1424154115cc7caf5f28b2e43f0b"

  container_port = 8080
  cpu            = 512
  memory         = 1024
  desired_count  = 1

  db_host                = module.rds.db_instance_address
  db_port                = 5432
  db_name                = module.rds.db_name
  db_user                = module.rds.db_username
  db_password_secret_arn = module.rds.db_password_secret_arn

  redis_host                = module.redis.redis_primary_endpoint_address
  redis_port                = module.redis.redis_port
  redis_password_secret_arn = module.redis.redis_password_secret_arn
}

