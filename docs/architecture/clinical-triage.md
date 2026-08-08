# Stage 13.1.2 — Enterprise Clinical Triage & Urgency Assessment Architecture

## Executive Summary

Stage 13.1.2 introduces **Enterprise Clinical Triage & Urgency Assessment** to the Vetra Clinical Intelligence module. Stage 13.1.3 extends triage to evaluate multi-modal evidence (critical lab results and critical vital signs).

The underlying **AI Platform v1.1 execution pipeline** (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and **Multi-Agent Framework** (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain **100% frozen, immutable, and preserved**.

---

## 1. 2-Layer Safety-First Architecture

Triage uses a strict 2-layer safety evaluation model:

```mermaid
flowchart TD
    Request[Multi-Modal Evidence Context] --> Layer1[Layer 1: ClinicalTriageRules<br/>Deterministic Safety Rules]
    
    Layer1 -->|Critical Indicator Detected<br/>e.g. respiratory distress, collapse, CRITICAL labs/vitals| Emergency[EMERGENCY Assessment<br/>Requires Immediate Vet Intervention]
    
    Layer1 -->|No Emergency Rules Triggered| Layer2[Layer 2: ClinicalTriageEngine<br/>AI TriageAgent via AgentGateway]
    
    Layer2 -->|Valid AI Response| Validated[Validated TriageAssessment<br/>EMERGENCY / URGENT / PRIORITY / ROUTINE]
    Layer2 -->|AI Failure or Invalid Response| Fallback[Conservative URGENT Fallback<br/>requiresImmediateReview = true]
```

### Safety Precedence Rules
1. **Deterministic Rule Precedence**: If a Layer 1 critical indicator triggers (respiratory distress, severe bleeding, seizures, collapse, or `AbnormalityStatus.CRITICAL` lab/vital signs), the system immediately returns `EMERGENCY` without invoking `TriageAgent` or `AgentGateway`.
2. **Negation Precedence**: Symptoms with negating prefixes (e.g. *"no respiratory distress"*, *"denies collapse"*, *"without bleeding"*) do **NOT** trigger emergency rules.
3. **Severe Disease vs Current Indicators**: High-severity disease candidates alone do not automatically trigger an emergency; classification depends on observable clinical indicators and vital/lab findings.
4. **Conservative Fallback Policy**: If AI reasoning fails or produces malformed JSON without a deterministic emergency, the engine returns a conservative `URGENT` assessment with `requiresImmediateVeterinaryReview = true` rather than defaulting to `ROUTINE`.

---

## 2. Standardized Urgency Levels

| Level | Definition | Escalation Action |
| :--- | :--- | :--- |
| **`EMERGENCY`** | Critical risk to life/welfare. | Seek immediate emergency veterinary intervention without delay. |
| **`URGENT`** | Significant risk requiring prompt assessment. | Contact veterinarian as soon as possible (preferably within hours). |
| **`PRIORITY`** | Moderate risk requiring short-term assessment. | Schedule veterinary examination within an appropriate short timeframe. |
| **`ROUTINE`** | Non-urgent clinical case. | Continue standard monitoring and routine care. |

---

## 3. Workflow Integration (Order 5)

`ClinicalTriageStep` executes at Order 5 in the 7-step clinical diagnosis pipeline:

1. **`DiagnosisStep`** (Order 1) → Visual pathology & anomaly detection.
2. **`EvidenceAggregationStep`** (Order 2) → Multi-modal evidence normalization & conflict detection.
3. **`KnowledgeStep`** (Order 3) → Grounded literature retrieval (RAG).
4. **`RankingStep`** (Order 4) → Multi-modal disease candidate ranking & confidence normalization.
5. **`ClinicalTriageStep`** (Order 5) → Deterministic rules + AI Triage assessment.
6. **`TreatmentStep`** (Order 6) → Treatment protocols respecting triage urgency.
7. **`ReportStep`** (Order 7) → Synthesis of `ClinicalDiagnosisReport` with Triage section.

---

## 4. Telemetry & Observability

### Micrometer Metrics
- **`clinical_triage_total`**: Counter tracking total triage assessments by `urgency` and `status`.
- **`clinical_triage_duration_seconds`**: Timer tracking triage execution duration.
- **`clinical_triage_urgency_total`**: Counter tracking assessments by urgency level.
- **`clinical_triage_escalations_total`**: Counter tracking `URGENT` and `EMERGENCY` escalations.

### OpenTelemetry Span Events
- `triage.started`
- `triage.completed`
- `triage.emergency`
- `triage.escalation_required`
