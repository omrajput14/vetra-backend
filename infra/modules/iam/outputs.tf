output "github_actions_role_arn" {
  description = "ARN of the IAM role assumed by GitHub Actions via OIDC"
  value       = aws_iam_role.github_actions_deploy.arn
}

output "github_actions_role_name" {
  description = "Name of the IAM role assumed by GitHub Actions via OIDC"
  value       = aws_iam_role.github_actions_deploy.name
}

output "oidc_provider_arn" {
  description = "ARN of the GitHub Actions OIDC Identity Provider"
  value       = aws_iam_openid_connect_provider.github.arn
}
