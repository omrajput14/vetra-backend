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
