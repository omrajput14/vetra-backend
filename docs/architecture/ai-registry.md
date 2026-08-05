# AI Registry & Configuration Framework

**Stage:** 13.0.2  
**Package:** `app.vetra.ai.registry`, `app.vetra.ai.config`

## Overview

The AI Registry Framework provides the configuration and routing infrastructure for the Vetra AI Gateway. It decouples business services from provider-specific implementations through a capability-based routing model backed by strongly typed YAML configuration.

No external AI provider calls are made in this layer.

---

## Component Responsibilities

| Component | Package | Responsibility |
| :--- | :--- | :--- |
| `AIGatewayProperties` | `config` | Typed binding of `vetra.ai.gateway.*` YAML |
| `ModelDescriptor` | `registry` | Immutable description of a registered model |
| `ModelRegistry` | `registry` | Alias-based model lookup, capability filtering |
| `ProviderRegistry` | `registry` | Provider discovery, name-based lookup, availability |
| `ProviderRouter` | `registry` | Capability-aware selection of provider + model |
| `AIRegistryValidator` | `registry` | Fail-fast `@PostConstruct` startup validation |
| `AIGatewayConfiguration` | `config` | Enables `AIGatewayProperties` Spring binding |

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                     application.yml                          │
│                  vetra.ai.gateway.*                          │
└─────────────────────┬────────────────────────────────────────┘
                      │ binds
                      ▼
           ┌──────────────────────┐
           │  AIGatewayProperties │  (immutable, validated)
           └──────┬───────┬───────┘
                  │       │
          models  │       │  providers
                  ▼       ▼
   ┌─────────────────┐  ┌──────────────────┐
   │  ModelRegistry  │  │  ProviderRegistry │
   │  ─────────────  │  │  ──────────────── │
   │  alias → Model  │  │  name → AIProvider│
   │  Descriptor     │  │  (by providerName)│
   └────────┬────────┘  └────────┬──────────┘
            │                    │
            └─────────┬──────────┘
                      │
                      ▼
             ┌────────────────┐
             │  ProviderRouter │
             │  ─────────────  │
             │  route(request) │
             │  RoutingDecision│
             └────────┬────────┘
                      │
                      ▼
             ┌────────────────┐
             │   AIGateway    │   ← Milestone 3
             │ (not yet built)│
             └────────────────┘
```

---

## Routing Flow

```
1. AIRequest arrives with promptId + requiredCapabilities

2. ProviderRouter.route(request)
   │
   ├── requiredCapabilities is EMPTY?
   │   └── → resolveDefault()
   │       Returns configured default provider + model
   │
   └── requiredCapabilities is NON-EMPTY?
       └── → resolveByCapabilities(required)
           │
           ├── ModelRegistry.findByCapabilities(required)
           │   → filter enabled models that support all capabilities
           │   → filter models whose provider is registered and available
           │
           ├── Prefer default model alias if in capable set
           │
           └── Select first capable model otherwise
               → ProviderRegistry.findByName(model.providerName())
               → return RoutingDecision(provider, model)
```

---

## Configuration Structure

```yaml
vetra:
  ai:
    gateway:
      default-provider: gemini          # Must match AIProvider.providerName()
      default-model: diagnostics-fast   # Must match a key in models map
      timeout: 10s

      providers:
        - name: gemini
          enabled: true
        - name: noop
          enabled: true

      models:
        diagnostics-fast:               # Alias — used by business logic
          provider: gemini              # References a provider name
          model-id: gemini-2.5-flash    # Provider-specific — hidden from business logic
          capabilities: [VISION, JSON_MODE]
          context-window: 1048576
          max-output-tokens: 8192
          enabled: true

        diagnostics-test:
          provider: noop
          model-id: noop-v1
          enabled: true
```

---

## Startup Validation

`AIRegistryValidator` runs at `@PostConstruct` after all Spring beans are initialized. If any rule fails, the application fails to start with a descriptive error listing all violations.

| Rule | Error Code | Condition |
| :--- | :--- | :--- |
| Default provider registered | `AI_CFG_011` | `default-provider` has no matching `AIProvider` bean |
| Default model registered | `AI_CFG_012` | `default-model` alias is not in `models` map |
| Model-provider cross-reference | `AI_CFG_013` | A model's `provider` field has no matching bean |
| Duplicate provider names | `AI_CFG_001` | Two `AIProvider` beans return same `providerName()` |
| Duplicate model aliases | `AI_CFG_003` | Two models have the same alias key |
| Unknown capability string | `AI_CFG_004` | Capability name doesn't match `AICapability` enum |

---

## Extension Guide: Adding a New Provider

Adding a new provider (e.g., Anthropic Claude) requires **zero changes** to existing registry code.

**Step 1:** Implement the `AIProvider` interface.

```java
@Component
public class ClaudeProvider implements AIProvider {
    @Override public String providerName() { return "claude"; }
    @Override public boolean isAvailable() { return true; }
    @Override public Set<AICapability> supportedCapabilities() {
        return Set.of(AICapability.VISION, AICapability.LONG_CONTEXT);
    }
    @Override public AIResponse execute(AIRequest request, String promptText) { /* ... */ }
    // ... deprecated bridge methods
}
```

**Step 2:** Add configuration.

```yaml
vetra:
  ai:
    gateway:
      default-provider: claude
      providers:
        - name: claude
          enabled: true
      models:
        analysis-deep:
          provider: claude
          model-id: claude-3-5-sonnet-20241022
          capabilities: [VISION, LONG_CONTEXT]
          context-window: 200000
          max-output-tokens: 8192
          enabled: true
```

**Step 3:** Spring auto-discovers the new bean. `ProviderRegistry` registers it by name. `ModelRegistry` maps aliases to it. `ProviderRouter` routes requests to it based on capabilities. `AIRegistryValidator` validates the configuration on startup. Done.

---

## NoOpAIProvider — First-Class Provider

`NoOpAIProvider` is promoted to a first-class provider (`providerName() = "noop"`). It is always registered in the `ProviderRegistry` and returns `isAvailable() = true`. It returns a deterministic stub `AIResponse` for CI and local development without making external calls. It declares no capabilities, ensuring it is never selected for capability-specific requests unless explicitly configured as default.
