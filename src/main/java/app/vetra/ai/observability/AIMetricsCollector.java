package app.vetra.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Consolidated operational metrics collector for the AI Gateway subsystem.
 *
 * <p>Strictly enforces low-cardinality tags (provider, model, promptId, promptVersion, status) and
 * records requests, latency histograms, governance rejections, token consumption, and cost estimates.
 */
@Service
public class AIMetricsCollector {

  private static final Logger log = LoggerFactory.getLogger(AIMetricsCollector.class);

  private final MeterRegistry meterRegistry;

  /**
   * Constructs AIMetricsCollector.
   *
   * @param meterRegistry Micrometer meter registry (optional-injected for test safety)
   */
  public AIMetricsCollector(@Autowired(required = false) MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * Records an AI request execution attempt and its latency.
   *
   * @param provider provider name
   * @param model model alias/ID
   * @param promptId prompt template ID
   * @param promptVersion prompt template version
   * @param status execution status (success/failure)
   * @param latencyNanos execution duration in nanoseconds
   */
  public void recordRequest(
      String provider,
      String model,
      String promptId,
      String promptVersion,
      String status,
      long latencyNanos) {

    String p = normalizeTagValue(provider, "UNKNOWN");
    String m = normalizeTagValue(model, "UNKNOWN");
    String pid = normalizeTagValue(promptId, "UNKNOWN");
    String pv = normalizeTagValue(promptVersion, "UNKNOWN");
    String st = normalizeTagValue(status, "UNKNOWN");

    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_REQUESTS_TOTAL)
          .description("Total AI gateway requests by status")
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Counter.builder(AIDashboardMetadata.METRIC_PROVIDER_REQUESTS_TOTAL)
          .description("Total requests by provider and status")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Counter.builder(AIDashboardMetadata.METRIC_MODEL_REQUESTS_TOTAL)
          .description("Total requests by model alias and status")
          .tag(AIDashboardMetadata.TAG_MODEL, m)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Timer.builder(AIDashboardMetadata.METRIC_REQUEST_DURATION)
          .description("AI request duration SLA timer")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_PROMPT_ID, pid)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(latencyNanos, TimeUnit.NANOSECONDS);

      Timer.builder(AIDashboardMetadata.METRIC_PROVIDER_LATENCY)
          .description("AI provider execution latency SLA timer")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_MODEL, m)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(latencyNanos, TimeUnit.NANOSECONDS);
    }
  }

  /**
   * Records an agent execution attempt, capability, result, and latency.
   *
   * @param agent agent name
   * @param capability capability executed
   * @param result result status (SUCCESS, CACHE_HIT, FAILED_PROVIDER, FAILED_GOVERNANCE, etc.)
   * @param latencyNanos duration in nanoseconds
   */
  public void recordAgentExecution(
      String agent, String capability, String result, long latencyNanos) {

    String ag = normalizeTagValue(agent, "UNKNOWN");
    String cap = normalizeTagValue(capability, "UNKNOWN");
    String res = normalizeTagValue(result, "UNKNOWN");

    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_AGENT_REQUESTS_TOTAL)
          .description("Total AI agent requests by agent, capability, and result")
          .tag(AIDashboardMetadata.TAG_AGENT, ag)
          .tag(AIDashboardMetadata.TAG_CAPABILITY, cap)
          .tag(AIDashboardMetadata.TAG_RESULT, res)
          .register(meterRegistry)
          .increment();

      Timer.builder(AIDashboardMetadata.METRIC_AGENT_DURATION)
          .description("AI agent execution latency timer")
          .tag(AIDashboardMetadata.TAG_AGENT, ag)
          .tag(AIDashboardMetadata.TAG_CAPABILITY, cap)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(latencyNanos, TimeUnit.NANOSECONDS);
    }
  }

  /** Records semantic search query execution, retrieved chunk counts, and retrieval latency. */
  public void recordRagQuery(int chunksRetrieved, double avgSimilarity, long latencyNanos) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_RAG_QUERIES_TOTAL)
          .description("Total veterinary RAG search queries executed")
          .register(meterRegistry).increment();

      Timer.builder(AIDashboardMetadata.METRIC_RAG_RETRIEVAL_LATENCY)
          .description("RAG semantic retrieval latency timer")
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry).record(latencyNanos, TimeUnit.NANOSECONDS);

      if (chunksRetrieved > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_RAG_RETRIEVED_CHUNKS)
            .description("Total chunks retrieved across RAG queries")
            .register(meterRegistry).increment(chunksRetrieved);
      }
    }
  }

  /** Records documents and chunks indexed into the vector store. */
  public void recordRagIngestion(int documentsCount, int chunksCount) {
    if (meterRegistry != null) {
      if (documentsCount > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_RAG_DOCUMENTS_INDEXED_TOTAL)
            .description("Total veterinary literature documents indexed")
            .register(meterRegistry).increment(documentsCount);
      }
      if (chunksCount > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_RAG_CHUNKS_INDEXED_TOTAL)
            .description("Total veterinary literature chunks indexed")
            .register(meterRegistry).increment(chunksCount);
      }
    }
  }

  /**
   * Records RAG context token volume injected into prompts.
   *
   * @param contextTokens token length of injected context
   */
  public void recordRagTokens(int contextTokens) {
    if (meterRegistry != null && contextTokens > 0) {
      Counter.builder(AIDashboardMetadata.METRIC_RAG_CONTEXT_TOKENS_TOTAL)
          .description("Total RAG context tokens injected into prompts")
          .register(meterRegistry)
          .increment(contextTokens);
    }
  }

  /**
   * Records a governance check rejection.
   *
   * @param governanceType safety, policy, or budget
   * @param promptId prompt ID
   * @param promptVersion prompt version
   */
  public void recordGovernanceRejection(
      String governanceType, String promptId, String promptVersion) {

    String gt = normalizeTagValue(governanceType, "UNKNOWN");
    String pid = normalizeTagValue(promptId, "UNKNOWN");
    String pv = normalizeTagValue(promptVersion, "UNKNOWN");

    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_GOVERNANCE_REJECTIONS_TOTAL)
          .description("Total AI governance rejections by type")
          .tag(AIDashboardMetadata.TAG_GOVERNANCE_TYPE, gt)
          .tag(AIDashboardMetadata.TAG_PROMPT_ID, pid)
          .tag(AIDashboardMetadata.TAG_PROMPT_VERSION, pv)
          .register(meterRegistry)
          .increment();
    }
  }

  /** Records prompt and completion token consumption. */
  public void recordTokenUsage(String provider, String model, int promptTokens, int completionTokens) {
    String p = normalizeTagValue(provider, "UNKNOWN");
    String m = normalizeTagValue(model, "UNKNOWN");
    if (meterRegistry != null) {
      if (promptTokens > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_PROMPT_TOKENS_TOTAL).description("Total AI prompt input tokens consumed").tag(AIDashboardMetadata.TAG_PROVIDER, p).tag(AIDashboardMetadata.TAG_MODEL, m).register(meterRegistry).increment(promptTokens);
      }
      if (completionTokens > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_COMPLETION_TOKENS_TOTAL).description("Total AI completion output tokens consumed").tag(AIDashboardMetadata.TAG_PROVIDER, p).tag(AIDashboardMetadata.TAG_MODEL, m).register(meterRegistry).increment(completionTokens);
      }
    }
  }

  /** Records estimated inference cost in USD. */
  public void recordCost(String provider, String model, double costUSD) {
    String p = normalizeTagValue(provider, "UNKNOWN");
    String m = normalizeTagValue(model, "UNKNOWN");
    if (meterRegistry != null && costUSD > 0.0) {
      Counter.builder(AIDashboardMetadata.METRIC_ESTIMATED_COST_TOTAL)
          .description("Estimated AI inference cost in USD")
          .tag(AIDashboardMetadata.TAG_PROVIDER, p)
          .tag(AIDashboardMetadata.TAG_MODEL, m)
          .register(meterRegistry).increment(costUSD);
    }
  }

  /**
   * Records a clinical diagnosis workflow execution attempt, status, and duration.
   *
   * @param status execution status (SUCCESS, FAILED, PARTIAL)
   * @param durationNanos duration in nanoseconds
   */
  public void recordClinicalWorkflow(String status, long durationNanos) {
    String st = normalizeTagValue(status, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_WORKFLOW_TOTAL)
          .description("Total multi-agent clinical diagnosis workflows executed")
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Timer.builder(AIDashboardMetadata.METRIC_CLINICAL_WORKFLOW_DURATION)
          .description("Clinical workflow end-to-end latency SLA timer")
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(durationNanos, TimeUnit.NANOSECONDS);
    }
  }

  /**
   * Records disease ranking and confidence normalization execution.
   *
   * @param status execution status (SUCCESS, FAILED)
   */
  public void recordDiseaseRanking(String status) {
    String st = normalizeTagValue(status, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_DISEASE_RANKING_TOTAL)
          .description("Total disease candidate ranking operations")
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();
    }
  }

  /**
   * Records treatment recommendation generation attempt.
   *
   * @param status execution status (SUCCESS, FAILED)
   */
  public void recordTreatmentGeneration(String status) {
    String st = normalizeTagValue(status, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_TREATMENT_GENERATION_TOTAL)
          .description("Total treatment generation operations")
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();
    }
  }

  /**
   * Records clinical triage assessment metrics.
   *
   * @param urgency determined urgency level (EMERGENCY, URGENT, PRIORITY, ROUTINE)
   * @param status execution status (SUCCESS, FAILED)
   * @param durationNanos execution duration in nanoseconds
   */
  public void recordClinicalTriage(String urgency, String status, long durationNanos) {
    String urg = normalizeTagValue(urgency, "UNKNOWN");
    String st = normalizeTagValue(status, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_TRIAGE_TOTAL)
          .description("Total clinical triage assessments executed")
          .tag(AIDashboardMetadata.TAG_URGENCY, urg)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .register(meterRegistry)
          .increment();

      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_TRIAGE_URGENCY_TOTAL)
          .description("Total triage assessments by urgency classification")
          .tag(AIDashboardMetadata.TAG_URGENCY, urg)
          .register(meterRegistry)
          .increment();

      if ("emergency".equals(urg) || "urgent".equals(urg)) {
        Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_TRIAGE_ESCALATIONS_TOTAL)
            .description("Total urgent/emergency clinical escalations")
            .tag(AIDashboardMetadata.TAG_URGENCY, urg)
            .register(meterRegistry)
            .increment();
      }

      Timer.builder(AIDashboardMetadata.METRIC_CLINICAL_TRIAGE_DURATION)
          .description("Clinical triage execution duration timer")
          .tag(AIDashboardMetadata.TAG_URGENCY, urg)
          .tag(AIDashboardMetadata.TAG_STATUS, st)
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(durationNanos, TimeUnit.NANOSECONDS);
    }
  }

  /**
   * Records multi-modal evidence processing metrics.
   *
   * @param itemCount total evidence items aggregated
   * @param conflictCount total measurement conflicts detected
   * @param durationMs processing latency in milliseconds
   */
  public void recordEvidenceProcessing(int itemCount, int conflictCount, long durationMs) {
    if (meterRegistry != null) {
      if (itemCount > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_MULTI_MODAL_EVIDENCE_TOTAL)
            .description("Total multi-modal evidence items aggregated")
            .register(meterRegistry)
            .increment(itemCount);
      }
      if (conflictCount > 0) {
        Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_EVIDENCE_CONFLICTS_TOTAL)
            .description("Total evidence measurement conflicts detected")
            .register(meterRegistry)
            .increment(conflictCount);
      }
      Timer.builder(AIDashboardMetadata.METRIC_MULTI_MODAL_EVIDENCE_DURATION)
          .description("Multi-modal evidence aggregation latency timer")
          .publishPercentiles(0.5, 0.95, 0.99)
          .register(meterRegistry)
          .record(durationMs, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Records clinical decision support explanation execution metrics.
   *
   * @param reviewRequired true if veterinarian review is required
   * @param uncertaintyLevel calculated uncertainty tier string
   */
  public void recordClinicalExplanation(boolean reviewRequired, String uncertaintyLevel) {
    String unc = normalizeTagValue(uncertaintyLevel, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_EXPLANATION_TOTAL)
          .description("Total clinical decision support explanations generated")
          .tag("review_required", String.valueOf(reviewRequired))
          .tag("uncertainty_level", unc)
          .register(meterRegistry)
          .increment();
    }
  }

  /**
   * Records clinical uncertainty metrics.
   *
   * @param level uncertainty level string
   */
  public void recordClinicalUncertainty(String level) {
    String unc = normalizeTagValue(level, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_UNCERTAINTY_TOTAL)
          .description("Total clinical uncertainty evaluations by tier")
          .tag("level", unc)
          .register(meterRegistry)
          .increment();
    }
  }

  /**
   * Records veterinarian review requirement triggers.
   *
   * @param reasonCategory structured reason category string
   */
  public void recordClinicalReviewRequired(String reasonCategory) {
    String cat = normalizeTagValue(reasonCategory, "UNKNOWN");
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_REVIEW_REQUIRED_TOTAL)
          .description("Total veterinarian review requirements triggered by category")
          .tag("reason_category", cat)
          .register(meterRegistry)
          .increment();
    }
  }

  public void recordClinicalActionPlan(String urgency, boolean vetRequired) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_ACTION_PLAN_TOTAL)
          .description("Total clinical action plans synthesized")
          .tag(AIDashboardMetadata.TAG_URGENCY, normalizeTagValue(urgency, "UNKNOWN"))
          .tag("review_required", String.valueOf(vetRequired)).register(meterRegistry).increment();
    }
  }

  public void recordClinicalCase(String status) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_CASES_TOTAL)
          .description("Total clinical cases managed by status")
          .tag("case_status", normalizeTagValue(status, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordClinicalEncounter(String type, String urgency) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_ENCOUNTERS_TOTAL)
          .description("Total clinical encounters attached by type and urgency")
          .tag("encounter_type", normalizeTagValue(type, "UNKNOWN"))
          .tag(AIDashboardMetadata.TAG_URGENCY, normalizeTagValue(urgency, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordTreatmentResponse(String responseStatus) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_TREATMENT_RESPONSE_TOTAL)
          .description("Total treatment responses evaluated by status")
          .tag("response_status", normalizeTagValue(responseStatus, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordConditionWorsened(String urgency) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_CONDITION_WORSENED_TOTAL)
          .description("Total clinical condition worsening escalations")
          .tag(AIDashboardMetadata.TAG_URGENCY, normalizeTagValue(urgency, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordCareTaskCreated(String type, String priority, String actor) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_CARE_TASKS_TOTAL)
          .description("Total clinical care tasks created")
          .tag("task_type", normalizeTagValue(type, "UNKNOWN"))
          .tag("priority", normalizeTagValue(priority, "UNKNOWN"))
          .tag("actor", normalizeTagValue(actor, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordCareTaskOverdue(String type, String priority) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_CARE_TASK_OVERDUE_TOTAL)
          .description("Total clinical care tasks overdue")
          .tag("task_type", normalizeTagValue(type, "UNKNOWN"))
          .tag("priority", normalizeTagValue(priority, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordCareTaskEscalated(String type, String priority) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_CARE_TASK_ESCALATION_TOTAL)
          .description("Total clinical care tasks escalated")
          .tag("task_type", normalizeTagValue(type, "UNKNOWN"))
          .tag("priority", normalizeTagValue(priority, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  public void recordCareTaskCompleted(String type, String priority, String actor) {
    if (meterRegistry != null) {
      Counter.builder(AIDashboardMetadata.METRIC_CLINICAL_CARE_TASK_COMPLETION_TOTAL)
          .description("Total clinical care tasks completed")
          .tag("task_type", normalizeTagValue(type, "UNKNOWN"))
          .tag("priority", normalizeTagValue(priority, "UNKNOWN"))
          .tag("actor", normalizeTagValue(actor, "UNKNOWN")).register(meterRegistry).increment();
    }
  }

  private String normalizeTagValue(String val, String fallback) {
    return (val == null || val.isBlank()) ? fallback : val.trim().toLowerCase();
  }
}
