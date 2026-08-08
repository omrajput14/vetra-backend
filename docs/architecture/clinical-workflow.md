# Stage 13.1.1 — Enterprise Clinical Diagnosis Workflow Architecture

## Executive Summary

Stage 13.1.1 establishes the **Enterprise Clinical Diagnosis Workflow** layer in Vetra AI Platform v1.1. Stage 13.1.4 extends this workflow to an 8-step multi-modal intelligence & explainability pipeline.

The underlying **AI Platform v1.1 execution pipeline** (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and the **Multi-Agent Framework** (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain **100% frozen, immutable, and preserved**.

---

## 1. 8-Step Multi-Modal Workflow Sequence

```mermaid
sequenceDiagram
    autonumber
    actor FarmerVet as Farmer / Veterinarian
    participant ScanService as AIScanService
    participant Engine as ClinicalWorkflowEngine
    participant Context as ClinicalWorkflowContext
    participant Gateway as AgentGateway
    participant Diagnosis as DiagnosisAgent (Vision)
    participant Aggregator as ClinicalEvidenceAggregator
    participant Knowledge as KnowledgeAgent (RAG)
    participant Ranker as DiseaseRanker
    participant Triage as ClinicalTriageEngine
    participant Treatment as TreatmentAgent
    participant CDSEngine as ClinicalDecisionSupportEngine
    participant ReportBuilder as ClinicalReportBuilder
    participant EventPub as ApplicationEventPublisher

    FarmerVet->>ScanService: createScan(imageUrl, symptoms, labs, vitals, sensors, history)
    ScanService->>Engine: executeWorkflow(ClinicalWorkflowRequest)
    Engine->>Context: initialize(request)
    Engine->>EventPub: publishEvent(ClinicalWorkflowStartedEvent)
    
    %% Step 1: Diagnosis
    rect rgb(240, 248, 255)
        note over Engine,Diagnosis: Step 1: DiagnosisStep
        Engine->>Gateway: execute(AgentRequest.DIAGNOSIS, vision)
        Gateway->>Diagnosis: execute()
        Diagnosis-->>Gateway: AgentResponse (conditions, observations)
        Gateway-->>Engine: AgentResponse
        Engine->>Context: setDiagnosisResponse(response)
    end

    %% Step 2: Evidence Aggregation
    rect rgb(245, 245, 255)
        note over Engine,Aggregator: Step 2: EvidenceAggregationStep
        Engine->>Aggregator: aggregateEvidence(symptoms, vision, labs, vitals, sensors, history)
        Aggregator-->>Engine: UnifiedClinicalEvidence (items, conflicts, summary)
        Engine->>Context: setUnifiedEvidence(unified)
    end

    %% Step 3: Knowledge RAG
    rect rgb(255, 250, 240)
        note over Engine,Knowledge: Step 3: KnowledgeStep (RAG)
        Engine->>Gateway: execute(AgentRequest.KNOWLEDGE, query, species, clinicalSummary)
        Gateway->>Knowledge: execute()
        Knowledge-->>Gateway: AgentResponse (literature context, citations)
        Gateway-->>Engine: AgentResponse
        Engine->>Context: setRetrievedContext(retrievedContext)
    end

    %% Step 4: Disease Ranking
    rect rgb(240, 255, 240)
        note over Engine,Ranker: Step 4: RankingStep
        Engine->>Ranker: rankDiseases(diagnosis, retrievedContext, symptoms, unifiedEvidence)
        Ranker-->>Engine: List<DiseaseCandidate> (normalized & sorted)
        Engine->>Context: setRankedDiseases(rankedCandidates)
    end

    %% Step 5: Clinical Triage & Urgency Assessment
    rect rgb(255, 235, 235)
        note over Engine,Triage: Step 5: ClinicalTriageStep
        Engine->>Triage: assessTriage(TriageRequest)
        Triage-->>Engine: TriageAssessment (urgency, rationale, warningSigns)
        Engine->>Context: setTriageAssessment(assessment)
    end

    %% Step 6: Treatment Coordination
    rect rgb(255, 245, 255)
        note over Engine,Treatment: Step 6: TreatmentStep
        Engine->>Gateway: execute(AgentRequest.TREATMENT, TreatmentRequest)
        Gateway->>Treatment: execute()
        Treatment-->>Gateway: AgentResponse (prescriptions, precautions, monitoring)
        Gateway-->>Engine: AgentResponse
        Engine->>Context: setTreatmentPlan(treatmentPlan)
    end

    %% Step 7: Clinical Decision Support & Explainability
    rect rgb(240, 240, 255)
        note over Engine,CDSEngine: Step 7: DecisionSupportStep
        Engine->>CDSEngine: evaluate(Context)
        CDSEngine-->>Engine: ClinicalDecisionSupport
        Engine->>Context: setDecisionSupport(decisionSupport)
    end

    %% Step 8: Report Synthesis
    rect rgb(255, 255, 240)
        note over Engine,ReportBuilder: Step 8: ReportStep
        Engine->>ReportBuilder: buildReport(context)
        ReportBuilder-->>Engine: ClinicalDiagnosisReport
        Engine->>Context: setReport(report)
    end

    Engine->>EventPub: publishEvent(ClinicalWorkflowCompletedEvent)
    Engine-->>ScanService: ClinicalWorkflowResult
```

---

## 2. Component Responsibilities

| Component | Layer / Package | Primary Responsibility |
| :--- | :--- | :--- |
| **`ClinicalWorkflowEngine`** | `app.vetra.ai.workflow.clinical` | Orchestrates polymorphic `WorkflowStep` components in order over a shared `ClinicalWorkflowContext`. |
| **`WorkflowStep`** | `app.vetra.ai.workflow.clinical.step` | Step abstraction enabling decoupled, testable, and parallelizable stages. |
| **`DiagnosisStep`** | `app.vetra.ai.workflow.clinical.step` | Step 1: Dispatches visual pathology and anomaly detection via `DiagnosisAgent`. |
| **`EvidenceAggregationStep`** | `app.vetra.ai.workflow.clinical.step` | Step 2: Normalizes multi-modal data streams, detects measurement conflicts, and produces `UnifiedClinicalEvidence`. |
| **`KnowledgeStep`** | `app.vetra.ai.workflow.clinical.step` | Step 3: Executes RAG retrieval via `KnowledgeAgent` using controlled clinical evidence summaries. |
| **`RankingStep`** | `app.vetra.ai.workflow.clinical.step` | Step 4: Invokes multi-modal `DiseaseRanker` with dynamic weight normalization across active modalities. |
| **`ClinicalTriageStep`** | `app.vetra.ai.workflow.clinical.step` | Step 5: Assesses case urgency via Layer 1 deterministic safety rules and Layer 2 `TriageAgent`. |
| **`TreatmentStep`** | `app.vetra.ai.workflow.clinical.step` | Step 6: Dispatches evidence-based treatment regimens via `TreatmentAgent`. |
| **`DecisionSupportStep`** | `app.vetra.ai.workflow.clinical.step` | Step 7: Deterministically synthesizes `ClinicalDecisionSupport`, evidence traceability, uncertainty, and vet review flags. |
| **`ReportStep`** | `app.vetra.ai.workflow.clinical.step` | Step 8: Invokes `ClinicalReportBuilder` to assemble the structured `ClinicalDiagnosisReport` with `ClinicalDecisionSupport`. |
| **`DiseaseRanker`** | `app.vetra.ai.workflow.clinical` | Multi-modal weighted candidate ranking with active modality normalization. |
| **`ClinicalTriageEngine`** | `app.vetra.ai.workflow.clinical.triage` | Evaluates deterministic safety rules + AI reasoning for urgency classification. |
| **`ClinicalDecisionSupportEngine`** | `app.vetra.ai.workflow.clinical.explainability` | Deterministic synthesis of evidence traceability, triage trigger explanation, uncertainty, and review flags. |
| **`ClinicalReportBuilder`** | `app.vetra.ai.workflow.clinical` | Pure report assembly component synthesizing outputs without containing business logic. |
| **`ClinicalWorkflowEventListener`** | `app.vetra.ai.workflow.clinical` | Asynchronous decoupled listener for workflow lifecycle domain events. |
