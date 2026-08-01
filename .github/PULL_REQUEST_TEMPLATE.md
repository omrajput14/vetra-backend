# Pull Request Description

## Summary of Changes
Provide a concise summary of the changes made in this Pull Request.

- 

## Impacted Bounded Context(s)
- [ ] `app.vetra.auth`
- [ ] `app.vetra.animal`
- [ ] `app.vetra.appointment`
- [ ] `app.vetra.medicalrecord`
- [ ] `app.vetra.ai`
- [ ] `app.vetra.disease`
- [ ] `app.vetra.notification`
- [ ] `app.vetra.dashboard`
- [ ] Infrastructure / Operations

## Quality & Compliance Checklist
- [ ] Code follows Google Java Style rules and `./mvnw checkstyle:check` passes with 0 violations.
- [ ] All unit and integration tests pass (`./mvnw clean test`).
- [ ] New/updated endpoints are documented with OpenAPI `@Operation` annotations.
- [ ] Flyway database migrations are forward-only and idempotent.
- [ ] Docker build succeeds locally (`docker compose build`).
- [ ] No hardcoded secrets, passwords, or private keys introduced.
- [ ] Architecture documentation (`docs/`) updated if necessary.
