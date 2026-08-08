# Stage 13.1.1 — Enterprise Clinical Diagnosis Workflow Architecture

## Executive Summary

Stage 13.1.1 establishes the **Enterprise Clinical Diagnosis Workflow** layer in Vetra AI Platform v1.1. It shifts the platform from single-request execution to an end-to-end multi-agent clinical pipeline that produces complete, veterinarian-ready clinical diagnosis reports.

The underlying **AI Platform v1.1 execution pipeline** (`DefaultAIGateway` → `AIGovernancePipeline` → `AICacheManager` → `FailoverManager` → `ProviderRouter` → `AIProvider`) and the **Multi-Agent Framework** (`AgentGateway` → `AgentRegistry` → `AIAgent`) remain **100% frozen, immutable, and preserved**.

---

## 1. Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor FarmerVet as Farmer / Veterinarian
    participant ScanService as AIScanService
    participant Engine as ClinicalWorkflowEngine
    participant Context as ClinicalWorkflowContext
    participant Gateway as AgentGateway
    participant Diagnosis as DiagnosisAgent (Vision)
    participant Knowledge as KnowledgeAgent (RAG)
    participant Ranker as DiseaseRanker
    participant Treatment as TreatmentAgent
    participant ReportBuilder as ClinicalReportBuilder
    participant EventPub as ApplicationEventPublisher

    FarmerVet->>ScanService: createScan(imageUrl, symptoms)
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
        Engine->>Engine: recordSpanEvent("diagnosis completed")
    end

    %% Step 2: Knowledge RAG
    rect rgb(255, 250, 240)
        note over Engine,Knowledge: Step 2: KnowledgeStep (RAG)
        Engine->>Gateway: execute(AgentRequest.KNOWLEDGE, query, species)
        Gateway->>Knowledge: execute()
        Knowledge-->>Gateway: AgentResponse (literature context, citations)
        Gateway-->>Engine: AgentResponse
        Engine->>Context: setRetrievedContext(retrievedContext)
        Engine->>Engine: recordSpanEvent("retrieval completed")
    end

    %% Step 3: Disease Ranking
    rect rgb(240, 255, 240)
        note over Engine,Ranker: Step 3: RankingStep
        Engine->>Ranker: rankDiseases(diagnosis, retrievedContext, symptoms)
        Ranker-->>Engine: List<DiseaseCandidate> (normalized & sorted)
        Engine->>Context: setRankedDiseases(rankedCandidates)
        Engine->>Engine: recordSpanEvent("ranking completed")
    end

    %% Step 4: Clinical Triage & Urgency Assessment
    rect rgb(255, 235, 235)
        note over Engine,Gateway: Step 4: ClinicalTriageStep
        Engine->>Gateway: assessTriage(TriageRequest)
        Gateway-->>Engine: TriageAssessment (urgency, rationale, warningSigns)
        Engine->>Context: setTriageAssessment(assessment)
        Engine->>Engine: recordSpanEvent("triage.completed")
    end

    %% Step 5: Treatment Coordination
    rect rgb(255, 245, 255)
        note over Engine,Treatment: Step 5: TreatmentStep
        Engine->>Gateway: execute(AgentRequest.TREATMENT, TreatmentRequest)
        Gateway->>Treatment: execute()
        Treatment-->>Gateway: AgentResponse (prescriptions, precautions, monitoring)
        Gateway-->>Engine: AgentResponse
        Engine->>Context: setTreatmentPlan(treatmentPlan)
        Engine->>Engine: recordSpanEvent("treatment completed")
    end

    %% Step 6: Report Synthesis
    rect rgb(255, 255, 240)
        note over Engine,ReportBuilder: Step 6: ReportStep
        Engine->>ReportBuilder: buildReport(context)
        ReportBuilder-->>Engine: ClinicalDiagnosisReport
        Engine->>Context: setReport(report)
        Engine->>Engine: recordSpanEvent("report generated")
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
| **`KnowledgeStep`** | `app.vetra.ai.workflow.clinical.step` | Step 2: Executes RAG retrieval via `KnowledgeAgent` with species metadata filtering. |
| **`RankingStep`** | `app.vetra.ai.workflow.clinical.step` | Step 3: Invokes `DiseaseRanker` to merge observations, normalize confidence scores, and deduplicate conditions. |
| **`ClinicalTriageStep`** | `app.vetra.ai.workflow.clinical.step` | Step 4: Assesses case urgency via Layer 1 deterministic safety rules and Layer 2 `TriageAgent`. |
| **`TreatmentStep`** | `app.vetra.ai.workflow.clinical.step` | Step 5: Dispatches evidence-based treatment regimens via `TreatmentAgent`. |
| **`ReportStep`** | `app.vetra.ai.workflow.clinical.step` | Step 6: Invokes `ClinicalReportBuilder` to assemble the structured `ClinicalDiagnosisReport`. |
| **`DiseaseRanker`** | `app.vetra.ai.workflow.clinical` | Merges visual diagnosis with literature citations and normalizes scores to `[0.00, 1.00]`. |
| **`ClinicalTriageEngine`** | `app.vetra.ai.workflow.clinical.triage` | Evaluates deterministic safety rules + AI reasoning for urgency classification. |
| **`ClinicalReportBuilder`** | `app.vetra.ai.workflow.clinical` | Pure report assembly component synthesizing outputs without containing business logic. |
| **`ClinicalWorkflowEventListener`** | `app.vetra.ai.workflow.clinical` | Asynchronous decoupled listener for workflow lifecycle domain events. |

---

## 3. Workflow Context & Data Models

### `ClinicalWorkflowContext`
The stateful context carrying intermediate state across the lifecycle:
- `request`: Input `ClinicalWorkflowRequest` (`scanId`, `animalId`, `imageUrl`, `species`, `symptoms`).
- `diagnosisResponse`: Raw visual output and condition observations.
- `retrievedContext`: Grounded veterinary text and structured `Citation` records from RAG.
- `rankedDiseases`: Sorted list of `DiseaseCandidate` records.
- `treatmentPlan`: Synthesized medications, dosage, biosecurity precautions, and monitoring guidance.
- `report`: Final `ClinicalDiagnosisReport`.
- `stepTimings`: Granular execution latency recorded per stage.
- `status`: `WorkflowStatus` (`RUNNING`, `SUCCESS`, `PARTIAL`, `FAILED`).

---

## 4. Observability & Low-Cardinality Telemetry

### Micrometer Metrics
- **`clinical_workflow_total`**: Counter tracking workflows by status (`SUCCESS`, `PARTIAL`, `FAILED`).
- **`clinical_workflow_duration_seconds`**: Latency SLA timer publishing percentiles (`p50`, `p95`, `p99`).
- **`disease_ranking_total`**: Counter tracking disease ranking executions.
- **`treatment_generation_total`**: Counter tracking treatment plan generations.

### OpenTelemetry Span Events
The workflow enriches the active span with canonical span events:
- `diagnosis completed`
- `retrieval completed`
- `ranking completed`
- `treatment completed`
- `report generated`

---

## 5. Future Extension Points

1. **Stage 13.1.2 — Automated Clinical Triage**:
   - Introduce `TriageStep` to calculate urgency index and trigger emergency SMS/push alerts.
2. **Stage 13.1.3 — Multi-Modal Lab & Sensor Integration**:
   - Extend `ClinicalWorkflowContext` with blood panel and IoT sensor telemetry (temperature, rumination rate).
3. **Parallel Step Execution**:
   - Independent steps (e.g. `DiagnosisStep` and preliminary `KnowledgeStep`) can execute concurrently via `CompletableFuture` without architectural modifications.
