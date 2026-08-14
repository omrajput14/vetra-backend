# ─────────────────────────────────────────────────────────────────────────────
# Module: redis — Main
# Stage:  14.5 — AWS Data Tier
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_elasticache_subnet_group" "this" {
  name        = "vetra-${var.environment}-redis-subnet-group"
  description = "Isolated subnets for Vetra ElastiCache Redis"
  subnet_ids  = var.isolated_subnet_ids

  tags = {
    Name        = "vetra-${var.environment}-redis-subnet-group"
    Environment = var.environment
  }
}

resource "random_password" "redis_password" {
  length  = 32
  special = true
  # Redis AUTH requires printable ASCII characters, avoid spaces or control characters
  override_special = "!&#$^<>-"
}

resource "aws_secretsmanager_secret" "redis_password" {
  name        = "/vetra/${var.environment}/redis/password"
  description = "Vetra ${var.environment} ElastiCache Redis AUTH token"

  recovery_window_in_days = 7

  tags = {
    Environment = var.environment
  }
}

resource "aws_secretsmanager_secret_version" "redis_password" {
  secret_id     = aws_secretsmanager_secret.redis_password.id
  secret_string = random_password.redis_password.result
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id = "vetra-${var.environment}-redis"
  description          = "Vetra ${var.environment} Redis Cluster"
  node_type            = var.node_type
  port                 = 6379
  parameter_group_name = "default.redis7"
  engine_version       = var.engine_version

  subnet_group_name  = aws_elasticache_subnet_group.this.name
  security_group_ids = [var.sg_redis_id]

  # Encryption and Authentication
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = aws_secretsmanager_secret_version.redis_password.secret_string
  kms_key_id                 = var.kms_key_id

  # HA/Cluster behavior
  num_cache_clusters         = var.num_cache_nodes
  automatic_failover_enabled = var.num_cache_nodes > 1

  tags = {
    Name        = "vetra-${var.environment}-redis"
    Environment = var.environment
  }

  lifecycle {
    ignore_changes = [
      auth_token # Ignore manual password rotations
    ]
  }
}
