output "redis_primary_endpoint_address" {
  description = "The primary endpoint address for the Redis cluster"
  value       = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "redis_reader_endpoint_address" {
  description = "The reader endpoint address for the Redis cluster (if num_cache_nodes > 1)"
  value       = aws_elasticache_replication_group.this.reader_endpoint_address
}

output "redis_port" {
  description = "The port used by the Redis cluster"
  value       = aws_elasticache_replication_group.this.port
}

output "redis_password_secret_arn" {
  description = "The ARN of the Secrets Manager secret containing the Redis AUTH token"
  value       = aws_secretsmanager_secret.redis_password.arn
}
