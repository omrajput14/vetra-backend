# ─────────────────────────────────────────────────────────────────────────────
# Module: vpc — Security Groups
# Stage:  14.2 — VPC / Network Architecture
#
# Security group least-privilege model (Stage 14.1 §8):
#
#   Internet → ALB SG (443/80) → ECS SG (8080) → RDS SG (5432)
#                                               → Redis SG (6379)
#
# IMPORTANT: Security group resources are declared with NO inline rules.
# Rules are attached with separate aws_security_group_rule resources.
# This breaks the circular dependency that arises when SGs reference each other.
#
# SSH (22) is NEVER allowed on any security group.
# ─────────────────────────────────────────────────────────────────────────────

# ── Security Group Shells (no inline rules) ───────────────────────────────────

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-sg-alb"
  description = "Application Load Balancer: accepts HTTPS/443 and HTTP/80 from internet only."
  vpc_id      = aws_vpc.this.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-sg-alb"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "ecs" {
  name        = "${local.name_prefix}-sg-ecs"
  description = "ECS Fargate tasks: accepts port 8080 from ALB only. No SSH. No internet ingress."
  vpc_id      = aws_vpc.this.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-sg-ecs"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-sg-rds"
  description = "RDS PostgreSQL: accepts port 5432 from ECS security group only."
  vpc_id      = aws_vpc.this.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-sg-rds"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-sg-redis"
  description = "ElastiCache Redis: accepts port 6379 from ECS security group only."
  vpc_id      = aws_vpc.this.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-sg-redis"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ── ALB Security Group Rules ───────────────────────────────────────────────────

resource "aws_security_group_rule" "alb_ingress_https" {
  security_group_id = aws_security_group.alb.id
  type              = "ingress"
  description       = "HTTPS from internet"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_ingress_http" {
  security_group_id = aws_security_group.alb.id
  type              = "ingress"
  description       = "HTTP from internet (redirect to HTTPS only, no application traffic)"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_egress_ecs" {
  security_group_id        = aws_security_group.alb.id
  type                     = "egress"
  description              = "Forward to ECS on application port 8080"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs.id
}

# ── ECS Security Group Rules ───────────────────────────────────────────────────

resource "aws_security_group_rule" "ecs_ingress_alb" {
  security_group_id        = aws_security_group.ecs.id
  type                     = "ingress"
  description              = "Application port 8080 from ALB only"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
}

resource "aws_security_group_rule" "ecs_egress_rds" {
  security_group_id        = aws_security_group.ecs.id
  type                     = "egress"
  description              = "PostgreSQL 5432 to RDS"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.rds.id
}

resource "aws_security_group_rule" "ecs_egress_redis" {
  security_group_id        = aws_security_group.ecs.id
  type                     = "egress"
  description              = "Redis 6379 to ElastiCache"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.redis.id
}

resource "aws_security_group_rule" "ecs_egress_https" {
  security_group_id = aws_security_group.ecs.id
  type              = "egress"
  description       = "HTTPS to VPC endpoints (Secrets Manager, ECR, S3) and internet (Gemini API via NAT)"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "ecs_egress_dns" {
  security_group_id = aws_security_group.ecs.id
  type              = "egress"
  description       = "DNS resolution via UDP/53"
  from_port         = 53
  to_port           = 53
  protocol          = "udp"
  cidr_blocks       = ["0.0.0.0/0"]
}

# ── RDS Security Group Rules ───────────────────────────────────────────────────

resource "aws_security_group_rule" "rds_ingress_ecs" {
  security_group_id        = aws_security_group.rds.id
  type                     = "ingress"
  description              = "PostgreSQL 5432 from ECS only"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs.id
}

# No egress rule for RDS — RDS initiates no outbound connections to application tier

# ── Redis Security Group Rules ────────────────────────────────────────────────

resource "aws_security_group_rule" "redis_ingress_ecs" {
  security_group_id        = aws_security_group.redis.id
  type                     = "ingress"
  description              = "Redis 6379 from ECS only"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs.id
}

# No egress rule for Redis — ElastiCache initiates no outbound connections to application tier
