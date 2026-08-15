# ─────────────────────────────────────────────────────────────────────────────
# Module: monitoring — Outputs
# Stage:  14.10 — Observability & Monitoring
# ─────────────────────────────────────────────────────────────────────────────

output "alerts_sns_topic_arn" {
  description = "ARN of the SNS topic for staging operational alarms"
  value       = aws_sns_topic.alerts.arn
}

output "alerts_sns_topic_name" {
  description = "Name of the SNS topic for staging operational alarms"
  value       = aws_sns_topic.alerts.name
}

output "dashboard_name" {
  description = "Name of the CloudWatch operational dashboard"
  value       = aws_cloudwatch_dashboard.staging.dashboard_name
}

output "dashboard_arn" {
  description = "ARN of the CloudWatch operational dashboard"
  value       = aws_cloudwatch_dashboard.staging.dashboard_arn
}

output "alarm_arns" {
  description = "Map of all provisioned CloudWatch Alarm ARNs"
  value = {
    ecs_cpu_high           = aws_cloudwatch_metric_alarm.ecs_cpu_high.arn
    ecs_memory_high        = aws_cloudwatch_metric_alarm.ecs_memory_high.arn
    alb_5xx_errors_high    = aws_cloudwatch_metric_alarm.alb_5xx_errors_high.arn
    alb_response_time_high = aws_cloudwatch_metric_alarm.alb_target_response_time_high.arn
    alb_unhealthy_hosts    = aws_cloudwatch_metric_alarm.alb_unhealthy_host_count.arn
    rds_cpu_high           = aws_cloudwatch_metric_alarm.rds_cpu_high.arn
    rds_storage_low        = aws_cloudwatch_metric_alarm.rds_storage_low.arn
    rds_connections_high   = aws_cloudwatch_metric_alarm.rds_connections_high.arn
    redis_cpu_high         = aws_cloudwatch_metric_alarm.redis_engine_cpu_high.arn
    redis_memory_high      = aws_cloudwatch_metric_alarm.redis_memory_high.arn
    redis_evictions        = aws_cloudwatch_metric_alarm.redis_evictions.arn
    app_error_logs         = aws_cloudwatch_metric_alarm.app_error_logs.arn
    app_db_timeouts        = aws_cloudwatch_metric_alarm.app_db_timeouts.arn
  }
}
