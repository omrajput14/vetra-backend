# Enterprise AI Governance, Safety & Cost Management

This document details the architectural design and execution mechanics of the Enterprise AI Governance layer in the Vetra backend.

## Overview

The AI Governance layer is designed to ensure that every AI request processed by `AIGateway` adheres to enterprise safety standards, tenant policy constraints, and in-memory cost/token budgets while producing privacy-preserving audit logs.

```text
Business Service
        │
        ▼
AIGateway.execute(AIRequest, AIExecutionContext)
        │
        ▼
AIGovernancePipeline
   ┌────┴──────────────────────────┐
   │ 1. AISafetyFilter             │ ──> (Throws AISafetyViolationException)
   │ 2. AIPolicyEngine             │ ──> (Throws AIPolicyViolationException)
   │ 3. AIBudgetManager (Check)    │ ──> (Throws AIBudgetExceededException)
   └────┬──────────────────────────┘
        ▼
FailoverManager (Provider Resilience & Retry)
        │
        ▼
   ┌────┴──────────────────────────┐
   │ 4. AIBudgetManager (Record)   │
   │ 5. AIAuditService (Log Event) │
   └───────────────────────────────┘
```

## Governance Components

### 1. `AIGovernancePipeline` & `DefaultAIGovernancePipeline`
Serves as the execution interceptor between `DefaultAIGateway` and `FailoverManager`. It enforces the evaluation order:
1. **Safety Filter**: Prompt content and parameter inspection.
2. **Policy Engine**: Tenant provider/model permission and prompt token upper bound verification.
3. **Budget Manager (Pre-flight)**: Validation of tenant daily token bounds.
4. **Provider Execution**: Delegated to `FailoverManager`.
5. **Budget Manager (Post-flight)**: Consumption accounting and estimated cost calculation.
6. **Audit Service**: Async/operational telemetry recording.

### 2. `AISafetyFilter`
- Evaluates rendered prompts against configured `blockedKeywords` and strictness levels.
- Pre-flight short-circuits execution before `FailoverManager` or any external provider call occurs.

### 3. `AIPolicyEngine`
- Fully configuration-driven via `vetra.ai.gateway.governance.policy`.
- Validates tenant-allowed providers (`tenantAllowedProviders`) and maximum prompt token bounds (`maxPromptTokens`).

### 4. `AIBudgetManager`
- Performs in-memory token limit validation, enforcement, and accounting.
- Does not introduce database or persistent billing side-effects.
- Calculates estimated cost per 1k tokens using configurable rates (`costPer1kTokens`).

### 5. `AIAuditService`
- Privacy-first operational logger emitting structured log events to `app.vetra.ai.audit`.
- Records operational metrics (`latencyMs`, `tokens`, `cost`, `provider`, `model`, `status`).
- Never persists API keys, credentials, or PII.
- Prompt text is logged strictly when `vetra.ai.gateway.governance.audit.log-prompt-content=true`.

### 6. `AIExecutionContext`
- Immutable context record carrying `tenantId`, `userId`, `correlationId`, and contextual `metadata`.
- Allows `AIRequest` and `AIResponse` public domain contracts to remain clean and provider-agnostic.

---

## Configuration Reference

```yaml
vetra:
  ai:
    gateway:
      governance:
        enabled: true
        safety:
          enabled: true
          blockedKeywords:
            - "prompt_injection"
            - "restricted_term"
          strictness: STRICT
        policy:
          enabled: true
          tenantAllowedProviders:
            tenant-a: ["gemini"]
          maxPromptTokens: 32768
        budget:
          enabled: true
          tenantDailyTokenLimit:
            tenant-a: 100000
          costPer1kTokens:
            gemini: 0.002
        audit:
          enabled: true
          logPromptContent: false
```

---

## Extension Guidelines

To add custom governance rules:
1. Create a custom rule component in `app.vetra.ai.gateway.governance`.
2. Inject your component into `DefaultAIGovernancePipeline`.
3. Add pre-flight validation calls prior to `executionChain.get()`.
