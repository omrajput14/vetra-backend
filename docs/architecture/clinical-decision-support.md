# Enterprise Clinical Decision Support & Explainability Architecture

## Executive Summary

Stage 13.1.4 introduces the **Enterprise Clinical Decision Support & Explainability** layer to the Vetra Clinical Intelligence Workflow. This layer renders the multi-agent clinical diagnosis pipeline fully explainable, auditable, uncertainty-aware, and traceable without adding extra AI agents, bypassing frozen platform infrastructure, or fabricating medical facts.

The underlying **AI Platform v1.1 execution pipeline** (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and **Multi-Agent Framework** (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain **100% frozen, immutable, and preserved**.

---

## 1. 8-Step Workflow Sequence

```text
DiagnosisStep (Order 1) ── Visual pathology inspection
      │
      ▼
EvidenceAggregationStep (Order 2) ── Normalizes 7 evidence streams & detects conflicts
      │
      ▼
KnowledgeStep (Order 3) ── Species-aware RAG literature retrieval
      │
      ▼
RankingStep (Order 4) ── Dynamic weighted candidate ranking
      │
      ▼
ClinicalTriageStep (Order 5) ── 2-Layer safety evaluation (Rules + AI)
      │
      ▼
TreatmentStep (Order 6) ── Evidence-based treatment planning
      │
      ▼
DecisionSupportStep (Order 7) ── Deterministic explainability, traceability, & review flags
      │
      ▼
ReportStep (Order 8) ── Assembles ClinicalDiagnosisReport with ClinicalDecisionSupport
```

---

## 2. Core Architectural Principles

1. **Strict Determinism**: `ClinicalDecisionSupportEngine` is a pure Spring component that derives explanations directly from structured workflow data (`UnifiedClinicalEvidence`, `DiseaseCandidate`, `TriageAssessment`, `TreatmentPlan`, `RetrievedContext`). No LLM call is executed to "explain itself."
2. **Explicit Safety Precedence**: If Layer 1 deterministic rules trigger an `EMERGENCY` triage classification, `TriageExplanation` identifies `triggerType = DETERMINISTIC_SAFETY_RULE` and sets `veterinarianReviewRequired = true`. An AI assessment cannot downgrade a deterministic emergency.
3. **Modality Contribution Reuse**: Uses normalized weights produced by `DiseaseRanker` from Stage 13.1.3 (Vision `0.35`, Labs `0.25`, Vitals `0.15`, Symptoms `0.15`, RAG `0.10`, normalized over active modalities).
4. **Missing Evidence as Uncertainty**: Missing evidence modalities (`LAB_RESULT`, `VITAL_SIGN`, `SENSOR_OBSERVATION`, `CLINICAL_HISTORY`, `RAG_LITERATURE`) are reported as missing modalities in `ClinicalUncertainty` rather than negative evidence.
5. **Conflict Preservation**: Reuses genuine measurement conflicts detected by `ClinicalEvidenceAggregator` in `ContradictoryEvidenceSummary`.
6. **Deterministic Veterinarian Review Flags**: Mandates professional review when `EMERGENCY` triage, low diagnostic confidence ($< 0.60$), insufficient evidence, genuine measurement conflicts, critical lab/vital abnormalities, or treatment precautions exist.

---

## 3. Observability & Safe Audit Metadata

### Micrometer Metrics
- **`clinical_explanation_total`**: Counter tagged with `review_required` and `uncertainty_level`.
- **`clinical_uncertainty_total`**: Counter tagged with `level`.
- **`clinical_review_required_total`**: Counter tagged with `reason_category`.

### OpenTelemetry Span Events
- `decision.support.started`
- `diagnosis.explanation.generated`
- `triage.explanation.generated`
- `uncertainty.detected`
- `veterinarian.review.required`

### Safe Audit Metadata
Strictly excludes PII, UUIDs, animal metrics, raw text, and prompt narratives. Contains only safe structural metadata: `engineVersion`, `workflowStepOrder`, `explanationStrategy`, `evaluatedAt`, `totalStepCount`.
