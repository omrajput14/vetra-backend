# ─────────────────────────────────────────────────────────────────────────────
# Module: vpc — Core Network Resources
# Stage:  14.2 — VPC / Network Architecture
# Region: ap-south-1 (Mumbai)
#
# Architecture (per environment):
#
#   Internet
#      │
#   Internet Gateway
#      │
#   ┌──┴────────────────────────────────────────────────────────────────────┐
#   │  VPC  10.x.0.0/16                                                    │
#   │                                                                       │
#   │  Public Subnets (ALB only — /24 each)                                │
#   │  ├── ap-south-1a: 10.x.1.0/24                                        │
#   │  └── ap-south-1b: 10.x.2.0/24                                        │
#   │                 │                                                     │
#   │             NAT Gateway(s)                                            │
#   │                 │                                                     │
#   │  Private Subnets (ECS Fargate — /24 each)                            │
#   │  ├── ap-south-1a: 10.x.10.0/24                                       │
#   │  └── ap-south-1b: 10.x.11.0/24                                       │
#   │                                                                       │
#   │  Isolated Data Subnets (RDS, ElastiCache — /24 each, no IGW, no NAT)│
#   │  ├── ap-south-1a: 10.x.20.0/24                                       │
#   │  └── ap-south-1b: 10.x.21.0/24                                       │
#   └───────────────────────────────────────────────────────────────────────┘
#
# Security baseline: docs/operations/aws-security.md (Stage 14.1)
# ─────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project}-${var.environment}"

  # Number of AZs used drives NAT Gateway count
  az_count = length(var.availability_zones)

  # Effective NAT count: 1 if single_nat_gateway, else one per AZ
  nat_count = var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : local.az_count) : 0

  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Stage       = "14.2"
    },
    var.tags
  )
}

# ── VPC ───────────────────────────────────────────────────────────────────────

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpc"
  })
}

# ── Internet Gateway ──────────────────────────────────────────────────────────

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-igw"
  })
}

# ── Public Subnets (ALB) ──────────────────────────────────────────────────────
# map_public_ip_on_launch = false — ALB itself gets Elastic IPs; instances
# (ECS Fargate) must never be placed in public subnets.

resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = false # ALB managed separately; never auto-assign

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-public-${var.availability_zones[count.index]}"
    Tier = "public"
    # Tag used by AWS Load Balancer Controller (future)
    "kubernetes.io/role/elb" = "1"
  })
}

# ── Private Subnets (ECS Fargate) ─────────────────────────────────────────────

resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.private_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = false # No public IPs on ECS tasks (Stage 14.1 §8)

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-private-${var.availability_zones[count.index]}"
    Tier = "private"
  })
}

# ── Isolated Data Subnets (RDS, ElastiCache) ─────────────────────────────────
# These subnets have no route to the internet gateway or NAT gateway.
# The only ingress is from the ECS security group on specific ports.

resource "aws_subnet" "isolated" {
  count = length(var.isolated_subnet_cidrs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.isolated_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = false

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-isolated-${var.availability_zones[count.index]}"
    Tier = "isolated"
  })
}

# ── Elastic IPs for NAT Gateways ──────────────────────────────────────────────

resource "aws_eip" "nat" {
  count  = local.nat_count
  domain = "vpc"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-nat-eip-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.this]
}

# ── NAT Gateways ─────────────────────────────────────────────────────────────
# Placed in public subnets. ECS tasks in private subnets route outbound
# traffic through NAT to reach ECR image pulls, Gemini API, etc.
# Production: one NAT per AZ for HA.
# Staging: single NAT to reduce cost (controlled by single_nat_gateway var).

resource "aws_nat_gateway" "this" {
  count = local.nat_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-nat-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.this]
}

# ── Route Tables ──────────────────────────────────────────────────────────────

# Public route table — default route via Internet Gateway
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rt-public"
  })
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Private route tables — one per AZ when multi-NAT, one shared when single-NAT
resource "aws_route_table" "private" {
  count  = local.az_count
  vpc_id = aws_vpc.this.id

  dynamic "route" {
    for_each = var.enable_nat_gateway ? [1] : []
    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = var.single_nat_gateway ? aws_nat_gateway.this[0].id : aws_nat_gateway.this[count.index].id
    }
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rt-private-${var.availability_zones[count.index]}"
  })
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# Isolated route tables — no internet route whatsoever
resource "aws_route_table" "isolated" {
  count  = local.az_count
  vpc_id = aws_vpc.this.id

  # No routes added — isolated subnets have local VPC routing only

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rt-isolated-${var.availability_zones[count.index]}"
  })
}

resource "aws_route_table_association" "isolated" {
  count = length(aws_subnet.isolated)

  subnet_id      = aws_subnet.isolated[count.index].id
  route_table_id = aws_route_table.isolated[count.index].id
}

# ── VPC Flow Logs ─────────────────────────────────────────────────────────────
# Required by Stage 14.1 §8. Sends all ACCEPT/REJECT traffic records to
# CloudWatch Logs for security analysis and incident investigation.

resource "aws_cloudwatch_log_group" "flow_logs" {
  count = var.enable_flow_logs ? 1 : 0

  name              = "/vetra/${var.environment}/vpc-flow-logs"
  retention_in_days = var.flow_log_retention_days

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-flow-log-group"
  })
}

resource "aws_iam_role" "flow_log" {
  count = var.enable_flow_logs ? 1 : 0

  name = "${local.name_prefix}-vpc-flow-log-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "vpc-flow-logs.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = local.common_tags
}

resource "aws_iam_role_policy" "flow_log" {
  count = var.enable_flow_logs ? 1 : 0

  name = "${local.name_prefix}-vpc-flow-log-policy"
  role = aws_iam_role.flow_log[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogGroups",
        "logs:DescribeLogStreams"
      ]
      Resource = aws_cloudwatch_log_group.flow_logs[0].arn
    }]
  })
}

resource "aws_flow_log" "this" {
  count = var.enable_flow_logs ? 1 : 0

  vpc_id          = aws_vpc.this.id
  traffic_type    = "ALL"
  iam_role_arn    = aws_iam_role.flow_log[0].arn
  log_destination = aws_cloudwatch_log_group.flow_logs[0].arn

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-flow-log"
  })
}

# ── S3 VPC Gateway Endpoint ───────────────────────────────────────────────────
# Required by Stage 14.1 §8. Routes S3 API calls through the AWS private
# network — no internet traversal. Free of charge (Gateway type).
# Attached to all route tables so all subnet tiers can reach S3 privately.

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids = concat(
    [aws_route_table.public.id],
    aws_route_table.private[*].id,
    aws_route_table.isolated[*].id
  )

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpce-s3"
  })
}

# ── Secrets Manager VPC Interface Endpoint ────────────────────────────────────
# Required by Stage 14.1 §8. Allows ECS tasks to fetch secrets from
# Secrets Manager without leaving the VPC. Interface type (ENIs in private
# subnets) — monthly per-endpoint and per-GB data charges apply.

resource "aws_security_group" "vpc_endpoints" {
  name        = "${local.name_prefix}-sg-vpce"
  description = "Security group for VPC interface endpoints (Secrets Manager, SSM). Allows HTTPS from within VPC only."
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTPS from VPC CIDR - allows ECS tasks to reach Secrets Manager endpoint"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "No outbound required from endpoint security group"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["127.0.0.1/32"] # Effectively denies all egress
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-sg-vpce"
  })
}

resource "aws_vpc_endpoint" "secretsmanager" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.aws_region}.secretsmanager"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpce-secretsmanager"
  })
}

# ── SSM VPC Interface Endpoint ────────────────────────────────────────────────
# Allows ECS tasks to reach SSM Parameter Store privately.

resource "aws_vpc_endpoint" "ssm" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.aws_region}.ssm"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpce-ssm"
  })
}

# ── ECR VPC Interface Endpoints ───────────────────────────────────────────────
# ECS Fargate pulls container images from ECR. Without these endpoints,
# image pulls would traverse the internet via the NAT Gateway.
# Two endpoints required: ecr.api (manifest fetch) and ecr.dkr (layer pull).

resource "aws_vpc_endpoint" "ecr_api" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.aws_region}.ecr.api"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpce-ecr-api"
  })
}

resource "aws_vpc_endpoint" "ecr_dkr" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.aws_region}.ecr.dkr"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpce-ecr-dkr"
  })
}

# ── CloudWatch Logs VPC Interface Endpoint ────────────────────────────────────
# Allows ECS containers to push logs to CloudWatch without NAT traversal.

resource "aws_vpc_endpoint" "logs" {
  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.aws_region}.logs"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-vpce-logs"
  })
}
