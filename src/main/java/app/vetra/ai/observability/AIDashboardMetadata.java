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

  // ── OpenTelemetry Span Events ─────────────────────────────────────────────
  public static final String SPAN_EVENT_DIAGNOSIS_COMPLETED = "diagnosis completed";
  public static final String SPAN_EVENT_RETRIEVAL_COMPLETED = "retrieval completed";
  public static final String SPAN_EVENT_RANKING_COMPLETED = "ranking completed";
  public static final String SPAN_EVENT_TRIAGE_STARTED = "triage.started";
  public static final String SPAN_EVENT_TRIAGE_COMPLETED = "triage.completed";
  public static final String SPAN_EVENT_TRIAGE_EMERGENCY = "triage.emergency";
  public static final String SPAN_EVENT_TRIAGE_ESCALATION = "triage.escalation_required";
  public static final String SPAN_EVENT_TREATMENT_COMPLETED = "treatment completed";
  public static final String SPAN_EVENT_REPORT_GENERATED = "report generated";

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
