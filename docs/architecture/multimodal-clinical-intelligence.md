# Enterprise Multi-Modal Clinical Intelligence Architecture

## Executive Summary

The **Vetra Multi-Modal Clinical Intelligence System** extends the Clinical Diagnosis Workflow by synthesizing 7 distinct evidence modalities into a unified reasoning context:
1. **Visual Pathology** (`IMAGE` via `DiagnosisAgent`)
2. **Clinical Symptoms** (`SYMPTOM`)
3. **Laboratory Test Results** (`LAB_RESULT` via `LaboratoryResult`)
4. **Physiological Vital Signs** (`VITAL_SIGN` via `VitalSign`)
5. **IoT Sensor Telemetry** (`SENSOR_OBSERVATION` via `SensorObservation`)
6. **Past Medical History** (`CLINICAL_HISTORY` via `ClinicalHistory`)
7. **Grounded Literature** (`RAG_LITERATURE` via `KnowledgeAgent`)

---

## 1. 7-Step Workflow Sequence

```text
DiagnosisStep (1) ── Visual pathology inspection
      │
      ▼
EvidenceAggregationStep (2) ── Normalizes multi-modal data & detects conflicts
      │
      ▼
KnowledgeStep (3) ── RAG literature search using controlled clinical summary
      │
      ▼
RankingStep (4) ── Dynamic weighted multi-modal disease ranking
      │
      ▼
ClinicalTriageStep (5) ── 2-Layer Triage (Rules + AI) with multi-modal checks
      │
      ▼
TreatmentStep (6) ── Evidence-based treatment planning
      │
      ▼
ReportStep (7) ── Synthesizes ClinicalDiagnosisReport with MultiModalEvidenceSummary
```

---

## 2. Multi-Modal Evidence & Data Quality

- **`ClinicalEvidenceAggregator`**:
  - Normalizes lab tests, vitals, sensor observations, symptoms, and medical history.
  - Distinguishes **genuine measurement conflicts** (e.g. concurrent temperature reading discrepancies $< 15$ mins) from **temporal measurement trends** ($> 30$ mins apart).
  - Handles missing/null values safely without fabricating clinical facts.
  - Excludes sensitive PII and raw metric payload text from telemetry tags.

---

## 3. Weighted Multi-Modal Ranking & Decoupled Triage

- **Engineering Default Modality Weights**:
  - Vision Weight: `0.35`
  - Laboratory Weight: `0.25`
  - Vital Signs Weight: `0.15`
  - Symptoms Weight: `0.15`
  - RAG Literature Weight: `0.10`
- **Dynamic Weight Normalization**: When a modality is missing, active weights are dynamically normalized to sum to `1.0`, preventing missing optional data from depressing candidate confidence.
- **Decoupled Confidence & Urgency**: Critical lab or vital sign abnormalities escalate triage urgency to `EMERGENCY` via Layer 1 deterministic safety rules, but do NOT automatically inflate disease confidence scores without explicit supporting evidence.

---

## 4. Telemetry & Low-Cardinality Metrics

| Metric / Span Event | Type | Low-Cardinality Tags / Description |
| :--- | :--- | :--- |
| **`multi_modal_evidence_total`** | Counter | Total evidence items aggregated |
| **`multi_modal_evidence_processing_duration_seconds`** | Timer | Evidence aggregation latency SLA |
| **`clinical_evidence_conflicts_total`** | Counter | Total measurement conflicts detected |
| **`evidence.aggregation.started`** | Span Event | Recorded upon entering `EvidenceAggregationStep` |
| **`evidence.aggregation.completed`** | Span Event | Recorded upon completing aggregation |
| **`evidence.conflict.detected`** | Span Event | Recorded when genuine measurement conflict is identified |
