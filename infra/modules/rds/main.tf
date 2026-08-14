# ─────────────────────────────────────────────────────────────────────────────
# Module: rds — Main
# Stage:  14.5 — AWS Data Tier
# ─────────────────────────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "this" {
  name        = "vetra-${var.environment}-rds-subnet-group"
  description = "Isolated subnets for Vetra RDS PostgreSQL"
  subnet_ids  = var.isolated_subnet_ids

  tags = {
    Name        = "vetra-${var.environment}-rds-subnet-group"
    Environment = var.environment
  }
}

resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_secretsmanager_secret" "db_password" {
  name        = "/vetra/${var.environment}/db/password"
  description = "Vetra ${var.environment} RDS PostgreSQL master password"

  # Force replacement if the environment changes
  recovery_window_in_days = 7

  tags = {
    Environment = var.environment
  }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db_password.result
}

resource "aws_db_instance" "this" {
  identifier = "vetra-${var.environment}-postgres"

  engine            = "postgres"
  engine_version    = var.engine_version
  instance_class    = var.instance_class
  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true
  kms_key_id        = var.kms_key_id

  db_name  = var.db_name
  username = var.db_user
  password = aws_secretsmanager_secret_version.db_password.secret_string

  vpc_security_group_ids = [var.sg_rds_id]
  db_subnet_group_name   = aws_db_subnet_group.this.name
  publicly_accessible    = false

  multi_az                = var.multi_az
  backup_retention_period = var.environment == "production" ? 7 : 1
  skip_final_snapshot     = var.environment == "production" ? false : true

  # Auto minor version upgrade enabled for security patching
  auto_minor_version_upgrade = true

  tags = {
    Name        = "vetra-${var.environment}-postgres"
    Environment = var.environment
  }

  lifecycle {
    ignore_changes = [
      password # Ignore manual password rotations
    ]
  }
}
