package app.vetra.ai.gateway.governance;

import app.vetra.ai.config.AIGatewayProperties;
import app.vetra.ai.config.GovernanceProperties;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.prompt.PromptDescriptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Privacy-first operational audit service. Records execution telemetry, latency, token usage,
 * cost estimates, and outcome status without persisting API keys, secrets, or PII. Prompt text is
 * logged strictly when enabled by explicit configuration.
 */
@Service
public class AIAuditService {

  private static final Logger AUDIT_LOG = LoggerFactory.getLogger("app.vetra.ai.audit");

  private final AIGatewayProperties properties;
  private final MeterRegistry meterRegistry;

  /**
   * Constructs AIAuditService with gateway properties and optional meter registry.
   *
   * @param properties gateway configuration properties
   * @param meterRegistry micrometer meter registry
   */
  @Autowired
  public AIAuditService(
      AIGatewayProperties properties, @Autowired(required = false) MeterRegistry meterRegistry) {
    this.properties = properties;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Records operational audit telemetry for a completed or failed request.
   *
   * @param request AI request
   * @param renderedPrompt rendered prompt template
   * @param descriptor prompt descriptor
   * @param response normalized response (may be null if failed)
   * @param context execution context
   * @param estimatedCost cost estimate in USD
   * @param latencyMs execution duration in ms
   * @param error error if execution failed (null if success)
   */
  public void recordAuditEvent(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      AIResponse response,
      AIExecutionContext context,
      double estimatedCost,
      long latencyMs,
      Throwable error) {

    GovernanceProperties.AuditConfig auditConfig = properties.getGovernance().getAudit();

    if (!properties.getGovernance().isEnabled() || !auditConfig.isEnabled()) {
      return;
    }

    String status = (error == null) ? "SUCCESS" : error.getClass().getSimpleName();
    String provider = (response != null) ? response.provider() : "UNKNOWN";
    String model = (response != null) ? response.model() : "UNKNOWN";
    int promptTokens = (response != null) ? response.promptTokens() : 0;
    int completionTokens = (response != null) ? response.completionTokens() : 0;
    int totalTokens = promptTokens + completionTokens;

    StringBuilder logMsg = new StringBuilder();
    logMsg.append("AI_AUDIT ");
    logMsg.append("correlationId=").append(context.correlationId()).append(" ");
    logMsg.append("tenantId=").append(context.tenantId()).append(" ");
    logMsg.append("userId=").append(context.userId()).append(" ");
    logMsg.append("promptId=").append(descriptor != null ? descriptor.promptId() : request.promptId()).append(" ");
    logMsg.append("promptVersion=").append(descriptor != null ? descriptor.version() : "unknown").append(" ");
    logMsg.append("provider=").append(provider).append(" ");
    logMsg.append("model=").append(model).append(" ");
    logMsg.append("latencyMs=").append(latencyMs).append(" ");
    logMsg.append("promptTokens=").append(promptTokens).append(" ");
    logMsg.append("completionTokens=").append(completionTokens).append(" ");
    logMsg.append("totalTokens=").append(totalTokens).append(" ");
    logMsg.append("estimatedCostUSD=").append(String.format("%.6f", estimatedCost)).append(" ");
    logMsg.append("status=").append(status);

    if (auditConfig.isLogPromptContent() && renderedPrompt != null) {
      logMsg.append(" promptContent=\"").append(sanitizeForAudit(renderedPrompt)).append("\"");
    }

    AUDIT_LOG.info(logMsg.toString());

    if (meterRegistry != null) {
      Counter.builder("ai_audit_events_total")
          .tag("tenantId", context.tenantId())
          .tag("promptId", descriptor != null ? descriptor.promptId() : "unknown")
          .tag("status", status)
          .register(meterRegistry)
          .increment();

      Counter.builder("ai_audit_tokens_total")
          .tag("tenantId", context.tenantId())
          .tag("provider", provider)
          .register(meterRegistry)
          .increment(totalTokens);
    }
  }

  private String sanitizeForAudit(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\n", "\\n").replace("\"", "\\\"");
  }
}
