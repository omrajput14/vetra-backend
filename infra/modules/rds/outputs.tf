output "db_instance_endpoint" {
  description = "The connection endpoint for the RDS instance"
  value       = aws_db_instance.this.endpoint
}

output "db_instance_id" {
  description = "The RDS instance ID"
  value       = aws_db_instance.this.id
}

output "db_name" {
  description = "The database name"
  value       = aws_db_instance.this.db_name
}

output "db_username" {
  description = "The master username for the database"
  value       = aws_db_instance.this.username
}

output "db_password_secret_arn" {
  description = "The ARN of the Secrets Manager secret containing the DB password"
  value       = aws_secretsmanager_secret.db_password.arn
}
