package app.vetra.ai.observability;

/**
 * Centralized constant definitions for AI operational metric names, tag keys, tag values, and
 * Prometheus metric identifiers. Prevents magic strings across instrumentation components.
 */
public final class AIDashboardMetadata {

  // ── Metric Names ──────────────────────────────────────────────────────────
  public static final String METRIC_REQUESTS_TOTAL = "ai_requests_total";
  public static final String METRIC_REQUEST_DURATION = "ai_request_duration_seconds";
  public static final String METRIC_PROVIDER_REQUESTS_TOTAL = "ai_provider_requests_total";
  public static final String METRIC_PROVIDER_LATENCY = "ai_provider_latency_seconds";
  public static final String METRIC_GOVERNANCE_REJECTIONS_TOTAL = "ai_governance_rejections_total";
  public static final String METRIC_PROMPT_TOKENS_TOTAL = "ai_prompt_tokens_total";
  public static final String METRIC_COMPLETION_TOKENS_TOTAL = "ai_completion_tokens_total";
  public static final String METRIC_ESTIMATED_COST_TOTAL = "ai_estimated_cost_total";
  public static final String METRIC_MODEL_REQUESTS_TOTAL = "ai_model_requests_total";
  public static final String METRIC_AGENT_REQUESTS_TOTAL = "ai_agent_requests_total";
  public static final String METRIC_AGENT_DURATION = "ai_agent_duration_seconds";
  public static final String METRIC_RAG_QUERIES_TOTAL = "rag_queries_total";
  public static final String METRIC_RAG_RETRIEVAL_LATENCY = "rag_retrieval_latency_seconds";
  public static final String METRIC_RAG_DOCUMENTS_INDEXED_TOTAL = "rag_documents_indexed_total";
  public static final String METRIC_RAG_CHUNKS_INDEXED_TOTAL = "rag_chunks_indexed_total";
  public static final String METRIC_RAG_CONTEXT_TOKENS_TOTAL = "rag_context_tokens_total";
  public static final String METRIC_RAG_AVG_SIMILARITY = "rag_avg_similarity_score";
  public static final String METRIC_RAG_RETRIEVED_CHUNKS = "rag_retrieved_chunks_total";
  public static final String METRIC_CLINICAL_WORKFLOW_TOTAL = "clinical_workflow_total";
  public static final String METRIC_CLINICAL_WORKFLOW_DURATION = "clinical_workflow_duration_seconds";
  public static final String METRIC_DISEASE_RANKING_TOTAL = "disease_ranking_total";
  public static final String METRIC_TREATMENT_GENERATION_TOTAL = "treatment_generation_total";
  public static final String METRIC_CLINICAL_TRIAGE_TOTAL = "clinical_triage_total";
  public static final String METRIC_CLINICAL_TRIAGE_DURATION = "clinical_triage_duration_seconds";
  public static final String METRIC_CLINICAL_TRIAGE_URGENCY_TOTAL = "clinical_triage_urgency_total";
  public static final String METRIC_CLINICAL_TRIAGE_ESCALATIONS_TOTAL = "clinical_triage_escalations_total";
  public static final String METRIC_MULTI_MODAL_EVIDENCE_TOTAL = "multi_modal_evidence_total";
  public static final String METRIC_MULTI_MODAL_EVIDENCE_DURATION = "multi_modal_evidence_processing_duration_seconds";
  public static final String METRIC_CLINICAL_EVIDENCE_CONFLICTS_TOTAL = "clinical_evidence_conflicts_total";
  public static final String METRIC_CLINICAL_EXPLANATION_TOTAL = "clinical_explanation_total";
  public static final String METRIC_CLINICAL_UNCERTAINTY_TOTAL = "clinical_uncertainty_total";
  public static final String METRIC_CLINICAL_REVIEW_REQUIRED_TOTAL = "clinical_review_required_total";
  public static final String METRIC_CLINICAL_ACTION_PLAN_TOTAL = "clinical_action_plan_total";
  public static final String METRIC_CLINICAL_ACTION_PLAN_DURATION = "clinical_action_plan_duration_seconds";
  public static final String METRIC_CLINICAL_ACTION_TOTAL = "clinical_action_total";
  public static final String METRIC_CLINICAL_ACTION_ESCALATION_TOTAL = "clinical_action_escalation_total";
  public static final String METRIC_CLINICAL_ACTION_VETERINARIAN_REVIEW_TOTAL = "clinical_action_veterinarian_review_total";
  public static final String METRIC_CLINICAL_CASES_TOTAL = "clinical_cases_total";
  public static final String METRIC_CLINICAL_ENCOUNTERS_TOTAL = "clinical_encounters_total";
  public static final String METRIC_CLINICAL_CASE_DURATION = "clinical_case_duration_seconds";
  public static final String METRIC_CLINICAL_FOLLOWUPS_TOTAL = "clinical_followups_total";
  public static final String METRIC_CLINICAL_FOLLOWUP_OVERDUE_TOTAL = "clinical_followup_overdue_total";
  public static final String METRIC_TREATMENT_RESPONSE_TOTAL = "treatment_response_total";
  public static final String METRIC_CLINICAL_CONDITION_WORSENED_TOTAL = "clinical_condition_worsened_total";
  public static final String METRIC_CLINICAL_CASE_RESOLUTION_TOTAL = "clinical_case_resolution_total";
  public static final String METRIC_CLINICAL_CARE_TASKS_TOTAL = "clinical_care_tasks_total";
  public static final String METRIC_CLINICAL_CARE_TASK_OVERDUE_TOTAL = "clinical_care_task_overdue_total";
  public static final String METRIC_CLINICAL_CARE_TASK_ESCALATION_TOTAL = "clinical_care_task_escalation_total";
  public static final String METRIC_CLINICAL_CARE_TASK_COMPLETION_TOTAL = "clinical_care_task_completion_total";
  public static final String METRIC_CLINICAL_FOLLOWUPS_DUE_TOTAL = "clinical_followups_due_total";
  public static final String METRIC_CLINICAL_FOLLOWUPS_MISSED_TOTAL = "clinical_followups_missed_total";
  public static final String METRIC_CLINICAL_OPERATIONS_DASHBOARD_TOTAL = "clinical_operations_dashboard_total";
  public static final String METRIC_CLINICAL_CASE_QUEUE_TOTAL = "clinical_case_queue_total";
  public static final String METRIC_CLINICAL_CARE_TASK_QUEUE_TOTAL = "clinical_care_task_queue_total";
  public static final String METRIC_CLINICAL_VETERINARIAN_QUEUE_TOTAL = "clinical_veterinarian_queue_total";

  // ── OpenTelemetry Span Events ─────────────────────────────────────────────
  public static final String SPAN_EVENT_DIAGNOSIS_COMPLETED = "diagnosis completed";
  public static final String SPAN_EVENT_EVIDENCE_AGGREGATION_STARTED = "evidence.aggregation.started";
  public static final String SPAN_EVENT_EVIDENCE_AGGREGATION_COMPLETED = "evidence.aggregation.completed";
  public static final String SPAN_EVENT_EVIDENCE_CONFLICT_DETECTED = "evidence.conflict.detected";
  public static final String SPAN_EVENT_RETRIEVAL_COMPLETED = "retrieval completed";
  public static final String SPAN_EVENT_RANKING_COMPLETED = "ranking completed";
  public static final String SPAN_EVENT_TRIAGE_STARTED = "triage.started";
  public static final String SPAN_EVENT_TRIAGE_COMPLETED = "triage.completed";
  public static final String SPAN_EVENT_TRIAGE_EMERGENCY = "triage.emergency";
  public static final String SPAN_EVENT_TRIAGE_ESCALATION = "triage.escalation_required";
  public static final String SPAN_EVENT_TREATMENT_COMPLETED = "treatment completed";
  public static final String SPAN_EVENT_DECISION_SUPPORT_STARTED = "decision.support.started";
  public static final String SPAN_EVENT_DIAGNOSIS_EXPLANATION_GENERATED = "diagnosis.explanation.generated";
  public static final String SPAN_EVENT_TRIAGE_EXPLANATION_GENERATED = "triage.explanation.generated";
  public static final String SPAN_EVENT_UNCERTAINTY_DETECTED = "uncertainty.detected";
  public static final String SPAN_EVENT_VETERINARIAN_REVIEW_REQUIRED = "veterinarian.review.required";
  public static final String SPAN_EVENT_ACTION_PLAN_STARTED = "action.plan.started";
  public static final String SPAN_EVENT_ACTION_PLAN_GENERATED = "action.plan.generated";
  public static final String SPAN_EVENT_ACTION_ESCALATION_REQUIRED = "action.escalation.required";
  public static final String SPAN_EVENT_ACTION_VETERINARIAN_REVIEW_REQUIRED = "action.veterinarian.review.required";
  public static final String SPAN_EVENT_ACTION_PLAN_COMPLETED = "action.plan.completed";
  public static final String SPAN_EVENT_REPORT_GENERATED = "report generated";

  // ── Span Events ────────────────────────────────────────────────────────────
  public static final String SPAN_EVENT_WORKFLOW_EXECUTION_STARTED = "workflow.execution.started";
  public static final String SPAN_EVENT_WORKFLOW_EXECUTION_COMPLETED = "workflow.execution.completed";
  public static final String SPAN_EVENT_WORKFLOW_STEP_COMPLETED = "workflow.step.completed";
  public static final String SPAN_EVENT_WORKFLOW_STEP_FAILED = "workflow.step.failed";
  public static final String SPAN_EVENT_CASE_CREATED = "case.created";
  public static final String SPAN_EVENT_ENCOUNTER_RECORDED = "encounter.recorded";
  public static final String SPAN_EVENT_CLINICAL_TIMELINE_UPDATED = "clinical.timeline.updated";
  public static final String SPAN_EVENT_FOLLOWUP_ASSESSMENT_STARTED = "followup.assessment.started";
  public static final String SPAN_EVENT_TREATMENT_RESPONSE_ASSESSED = "treatment.response.assessed";
  public static final String SPAN_EVENT_CLINICAL_CONDITION_WORSENED = "clinical.condition.worsened";
  public static final String SPAN_EVENT_CLINICAL_CASE_RESOLVED = "clinical.case.resolved";
  public static final String SPAN_EVENT_CARE_TASK_CREATED = "care.task.created";
  public static final String SPAN_EVENT_CARE_TASK_ASSIGNED = "care.task.assigned";
  public static final String SPAN_EVENT_CARE_TASK_STARTED = "care.task.started";
  public static final String SPAN_EVENT_CARE_TASK_COMPLETED = "care.task.completed";
  public static final String SPAN_EVENT_CARE_TASK_OVERDUE = "care.task.overdue";
  public static final String SPAN_EVENT_CARE_TASK_ESCALATED = "care.task.escalated";
  public static final String SPAN_EVENT_FOLLOWUP_DUE = "followup.due";
  public static final String SPAN_EVENT_FOLLOWUP_MISSED = "followup.missed";

  // ── Tag Keys ──────────────────────────────────────────────────────────────
  public static final String TAG_PROVIDER = "provider";
  public static final String TAG_MODEL = "model";
  public static final String TAG_PROMPT_ID = "prompt_id";
  public static final String TAG_PROMPT_VERSION = "prompt_version";
  public static final String TAG_CACHE_RESULT = "cache_result";
  public static final String TAG_GOVERNANCE_TYPE = "governance_type";
  public static final String TAG_STATUS = "status";
  public static final String TAG_AGENT = "agent";
  public static final String TAG_CAPABILITY = "capability";
  public static final String TAG_RESULT = "result";
  public static final String TAG_URGENCY = "urgency";
  public static final String TAG_TASK_TYPE = "task_type";
  public static final String TAG_PRIORITY = "priority";
  public static final String TAG_ACTOR = "actor";

  // ── Tag Values ────────────────────────────────────────────────────────────
  public static final String STATUS_SUCCESS = "success";
  public static final String STATUS_FAILURE = "failure";
  public static final String STATUS_REJECTED = "rejected";

  public static final String GOVERNANCE_SAFETY = "safety";
  public static final String GOVERNANCE_POLICY = "policy";
  public static final String GOVERNANCE_BUDGET = "budget";

  private AIDashboardMetadata() {
    // Utility class private constructor
  }
}
