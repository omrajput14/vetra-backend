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

# ── RDS PostgreSQL ────────────────────────────────────────────────────────────

module "rds" {
  source = "../../modules/rds"

  environment = "production"

  vpc_id              = module.vpc.vpc_id
  isolated_subnet_ids = module.vpc.isolated_subnet_ids
  sg_rds_id           = module.vpc.sg_rds_id

  instance_class    = "db.t4g.medium" # HA Production DB
  allocated_storage = 100
  multi_az          = true
  engine_version    = "15.13"
}

# ── ElastiCache Redis ─────────────────────────────────────────────────────────

module "redis" {
  source = "../../modules/redis"

  environment = "production"

  vpc_id              = module.vpc.vpc_id
  isolated_subnet_ids = module.vpc.isolated_subnet_ids
  sg_redis_id         = module.vpc.sg_redis_id

  node_type       = "cache.t4g.medium"
  num_cache_nodes = 2 # Automatic failover enabled
}

# ── IAM / OIDC GitHub Actions Deployment Role ─────────────────────────────────

module "iam" {
  source = "../../modules/iam"

  environment        = "production"
  project            = "vetra"
  github_repository  = "omrajput14/vetra-backend"
  github_branch      = "refs/heads/production"
  ecr_repository_arn = module.ecr.repository_arn
}

# ── ECS Fargate & Application Load Balancer ───────────────────────────────────

module "ecs" {
  source = "../../modules/ecs"

  environment = "production"
  project     = "vetra"
  aws_region  = "ap-south-1"

  vpc_id             = module.vpc.vpc_id
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids
  sg_alb_id          = module.vpc.sg_alb_id
  sg_ecs_id          = module.vpc.sg_ecs_id

  ecr_repository_url = module.ecr.repository_url
  image_tag          = "production-initial"

  container_port = 8080
  cpu            = 1024 # 1 vCPU for production baseline
  memory         = 2048 # 2 GB RAM for production baseline
  desired_count  = 2    # Multi-AZ baseline: 2 instances minimum across AZs

  db_host                = module.rds.db_instance_address
  db_port                = 5432
  db_name                = module.rds.db_name
  db_user                = module.rds.db_username
  db_password_secret_arn = module.rds.db_password_secret_arn

  redis_host                = module.redis.redis_primary_endpoint_address
  redis_port                = module.redis.redis_port
  redis_password_secret_arn = module.redis.redis_password_secret_arn

  # Application Auto Scaling (Stage 14.11 / 14.12)
  enable_autoscaling                = true
  autoscaling_min_capacity          = 2
  autoscaling_max_capacity          = 10
  autoscaling_cpu_target_percentage = 70.0
  autoscaling_request_count_target  = 1000
  autoscaling_scale_out_cooldown    = 60
  autoscaling_scale_in_cooldown     = 300

  # HTTPS & SSL/TLS (Stage 14.8 / 14.12)
  # ACM Certificate ARN placeholder: populate when production domain certificate is validated in ACM.
  # When empty (""), the ALB listener defaults to HTTP forwarding on port 80.
  certificate_arn = ""
}

# ── Observability & Monitoring (Stage 14.10 / 14.12) ──────────────────────────

module "monitoring" {
  source = "../../modules/monitoring"

  environment = "production"
  project     = "vetra"
  aws_region  = "ap-south-1"

  ecs_cluster_name           = module.ecs.cluster_name
  ecs_service_name           = module.ecs.service_name
  alb_arn_suffix             = module.ecs.alb_arn_suffix
  target_group_arn_suffix    = module.ecs.target_group_arn_suffix
  db_instance_id             = module.rds.db_instance_id
  redis_replication_group_id = module.redis.replication_group_id
  ecs_log_group_name         = module.ecs.log_group_name
}
