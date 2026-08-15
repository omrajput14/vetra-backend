# ─────────────────────────────────────────────────────────────────────────────
# Module: monitoring — CloudWatch Alarms, Metric Filters & Dashboard
# Stage:  14.10 — Observability & Monitoring
# ─────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project}-${var.environment}"
  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
      Stage       = "14.10"
    },
    var.tags
  )
  metric_namespace = "Vetra/${title(var.environment)}/Application"
}

# ── 1. SNS Alert Notification Topic ──────────────────────────────────────────

resource "aws_sns_topic" "alerts" {
  name = "${local.name_prefix}-alerts-topic"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alerts-topic"
  })
}

resource "aws_sns_topic_subscription" "email" {
  count = var.alert_email != "" ? 1 : 0

  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# ── 2. ECS Fargate Alarms ────────────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  alarm_name          = "${local.name_prefix}-ecs-cpu-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "ECS service CPU utilization exceeds 80% for 2 consecutive minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "ecs_memory_high" {
  alarm_name          = "${local.name_prefix}-ecs-memory-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = 60
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "ECS service memory utilization exceeds 80% for 2 consecutive minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  tags = local.common_tags
}

# ── 3. Application Load Balancer Alarms ───────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "alb_5xx_errors_high" {
  alarm_name          = "${local.name_prefix}-alb-target-5xx-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Sum"
  threshold           = 5
  treat_missing_data  = "notBreaching"
  alarm_description   = "ALB target generated 5 or more HTTP 5XX server errors within 1 minute"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.target_group_arn_suffix
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "alb_target_response_time_high" {
  alarm_name          = "${local.name_prefix}-alb-target-response-time-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  extended_statistic  = "p95"
  threshold           = 2.0
  treat_missing_data  = "notBreaching"
  alarm_description   = "ALB p95 target response time exceeds 2.0 seconds for 2 consecutive minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.target_group_arn_suffix
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "alb_unhealthy_host_count" {
  alarm_name          = "${local.name_prefix}-alb-unhealthy-hosts"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 3
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Maximum"
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_description   = "ALB Target Group has 1 or more unhealthy hosts for 3 consecutive minutes (sustained outage beyond rolling deployment window)"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.target_group_arn_suffix
  }

  tags = local.common_tags
}


# ── 4. RDS PostgreSQL Alarms ──────────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "${local.name_prefix}-rds-cpu-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "RDS instance CPU utilization exceeds 80% for 10 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = var.db_instance_id
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "rds_storage_low" {
  alarm_name          = "${local.name_prefix}-rds-storage-low"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 5368709120 # 5 GB in bytes
  alarm_description   = "RDS free storage space dropped below 5 GB"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = var.db_instance_id
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "rds_connections_high" {
  alarm_name          = "${local.name_prefix}-rds-connections-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "RDS active database connections exceeded 80"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = var.db_instance_id
  }

  tags = local.common_tags
}

# ── 5. ElastiCache Redis Alarms ───────────────────────────────────────────────

resource "aws_cloudwatch_metric_alarm" "redis_engine_cpu_high" {
  alarm_name          = "${local.name_prefix}-redis-engine-cpu-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "EngineCPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 75
  alarm_description   = "Redis Engine CPU utilization exceeded 75% for 10 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    CacheClusterId = "${var.redis_replication_group_id}-001"
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_memory_high" {
  alarm_name          = "${local.name_prefix}-redis-memory-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "DatabaseMemoryUsagePercentage"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "Redis cache memory utilization exceeded 80% for 10 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    CacheClusterId = "${var.redis_replication_group_id}-001"
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  alarm_name          = "${local.name_prefix}-redis-evictions"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "Evictions"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_description   = "Redis has started evicting keys due to memory pressure"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    CacheClusterId = "${var.redis_replication_group_id}-001"
  }

  tags = local.common_tags
}

# ── 6. CloudWatch Logs Metric Filters & Application Alarms ────────────────────

resource "aws_cloudwatch_log_metric_filter" "app_errors" {
  name           = "${local.name_prefix}-app-errors-filter"
  log_group_name = var.ecs_log_group_name
  pattern        = "?ERROR ?Exception ?\"level=ERROR\""

  metric_transformation {
    name          = "ApplicationErrors"
    namespace     = local.metric_namespace
    value         = "1"
    default_value = 0
  }
}

resource "aws_cloudwatch_metric_alarm" "app_error_logs" {
  alarm_name          = "${local.name_prefix}-app-error-logs-spike"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = aws_cloudwatch_log_metric_filter.app_errors.metric_transformation[0].name
  namespace           = aws_cloudwatch_log_metric_filter.app_errors.metric_transformation[0].namespace
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  treat_missing_data  = "notBreaching"
  alarm_description   = "Spring Boot container logged 5 or more ERROR-level log events within 5 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  tags = local.common_tags
}

resource "aws_cloudwatch_log_metric_filter" "db_timeouts" {
  name           = "${local.name_prefix}-db-timeouts-filter"
  log_group_name = var.ecs_log_group_name
  pattern        = "?\"Connection is not available, request timed out\" ?\"SQLTransientConnectionException\""

  metric_transformation {
    name          = "DatabaseConnectionTimeouts"
    namespace     = local.metric_namespace
    value         = "1"
    default_value = 0
  }
}

resource "aws_cloudwatch_metric_alarm" "app_db_timeouts" {
  alarm_name          = "${local.name_prefix}-app-db-connection-timeouts"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = aws_cloudwatch_log_metric_filter.db_timeouts.metric_transformation[0].name
  namespace           = aws_cloudwatch_log_metric_filter.db_timeouts.metric_transformation[0].namespace
  period              = 60
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_description   = "HikariCP connection pool timeout occurred in Spring Boot application"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  tags = local.common_tags
}

# ── 7. Operational CloudWatch Dashboard ───────────────────────────────────────

resource "aws_cloudwatch_dashboard" "staging" {
  dashboard_name = "${local.name_prefix}-operations-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ServiceName", var.ecs_service_name, "ClusterName", var.ecs_cluster_name, { stat = "Average", label = "ECS CPU Utilization (%)", color = "#ff7f0e" }],
            ["AWS/ECS", "MemoryUtilization", "ServiceName", var.ecs_service_name, "ClusterName", var.ecs_cluster_name, { stat = "Average", label = "ECS Memory Utilization (%)", color = "#1f77b4" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "ECS Fargate Compute Utilization"
          period  = 60
          yAxis = {
            left = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", var.alb_arn_suffix, { stat = "Sum", label = "Total Requests", color = "#2ca02c" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "p95", label = "p95 Target Response Time (s)", color = "#d62728", yAxis = "right" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "p50", label = "p50 Target Response Time (s)", color = "#9467bd", yAxis = "right" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "ALB Traffic & Latency"
          period  = 60
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_2XX_Count", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "Sum", label = "Target 2XX", color = "#2ca02c" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_4XX_Count", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "Sum", label = "Target 4XX", color = "#ffbb78" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "Sum", label = "Target 5XX", color = "#d62728" }],
            ["AWS/ApplicationELB", "HTTPCode_ELB_5XX_Count", "LoadBalancer", var.alb_arn_suffix, { stat = "Sum", label = "ELB 5XX", color = "#8c564b" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "ALB HTTP Response Codes"
          period  = 60
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "HealthyHostCount", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "Minimum", label = "Healthy Hosts", color = "#2ca02c" }],
            ["AWS/ApplicationELB", "UnHealthyHostCount", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.target_group_arn_suffix, { stat = "Maximum", label = "Unhealthy Hosts", color = "#d62728" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "ALB Target Group Host Health"
          period  = 60
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", var.db_instance_id, { stat = "Average", label = "RDS CPU (%)", color = "#ff7f0e" }],
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", var.db_instance_id, { stat = "Average", label = "Active Connections", color = "#1f77b4", yAxis = "right" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "RDS PostgreSQL Resource Utilization"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ElastiCache", "EngineCPUUtilization", "CacheClusterId", "${var.redis_replication_group_id}-001", { stat = "Average", label = "Redis Engine CPU (%)", color = "#ff7f0e" }],
            ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", "CacheClusterId", "${var.redis_replication_group_id}-001", { stat = "Average", label = "Redis Memory Usage (%)", color = "#1f77b4" }],
            ["AWS/ElastiCache", "CurrConnections", "CacheClusterId", "${var.redis_replication_group_id}-001", { stat = "Average", label = "Redis Connections", color = "#2ca02c", yAxis = "right" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "ElastiCache Redis Performance"
          period  = 300
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 18
        width  = 12
        height = 6
        properties = {
          metrics = [
            [local.metric_namespace, "ApplicationErrors", { stat = "Sum", label = "Application Errors", color = "#d62728" }],
            [local.metric_namespace, "DatabaseConnectionTimeouts", { stat = "Sum", label = "Hikari Connection Timeouts", color = "#ff7f0e" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Application Log Metric Filter Events"
          period  = 60
        }
      },
      {
        type   = "log"
        x      = 12
        y      = 18
        width  = 12
        height = 6
        properties = {
          query  = "SOURCE '${var.ecs_log_group_name}' | fields @timestamp, @message | filter @message like /(?i)error|exception|fail/ | sort @timestamp desc | limit 20"
          region = var.aws_region
          title  = "Recent Application Error Logs"
          view   = "table"
        }
      }
    ]
  })
}
