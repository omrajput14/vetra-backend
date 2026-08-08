package app.vetra.ai.agent.gateway;

import app.vetra.ai.agent.AIAgent;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.agent.registry.AgentRegistry;
import app.vetra.ai.exception.AIConfigurationException;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link AgentGateway}.
 *
 * <p>Directly queries {@link AgentRegistry} to resolve specialized agents, coordinates execution,
 * and records agent-level operational telemetry.
 */
@Service
public class DefaultAgentGateway implements AgentGateway {

  private static final Logger log = LoggerFactory.getLogger(DefaultAgentGateway.class);

  private final AgentRegistry agentRegistry;
  private final AIMetricsCollector metricsCollector;
  private final AIObservationConvention observationConvention;

  /**
   * Constructs DefaultAgentGateway.
   *
   * @param agentRegistry registry containing discovered agents
   * @param metricsCollector operational metrics collector (optional)
   * @param observationConvention OpenTelemetry tracer convention helper (optional)
   */
  public DefaultAgentGateway(
      AgentRegistry agentRegistry,
      @Autowired(required = false) AIMetricsCollector metricsCollector,
      @Autowired(required = false) AIObservationConvention observationConvention) {
    this.agentRegistry = agentRegistry;
    this.metricsCollector = metricsCollector;
    this.observationConvention = observationConvention;
  }

  @Override
  public AgentResponse execute(AgentRequest request) {
    if (request == null || request.capability() == null) {
      throw new AIConfigurationException(
          "Invalid AgentRequest: capability must not be null", "AGENT_INVALID_REQUEST");
    }

    AgentCapability capability = request.capability();
    log.info("AgentGateway resolving agent for capability={}", capability);

    AIAgent agent =
        agentRegistry
            .findHealthyAgent(capability)
            .orElseThrow(
                () ->
                    new AIConfigurationException(
                        "No healthy agent available for capability: " + capability,
                        "AGENT_UNAVAILABLE"));

    log.info("AgentGateway selected agent '{}' for capability={}", agent.agentName(), capability);

    if (observationConvention != null) {
      observationConvention.recordSpanEvent("ai.agent.selected");
    }

    long startNanos = System.nanoTime();
    try {
      AgentResponse response = agent.execute(request);
      long durationNanos = System.nanoTime() - startNanos;

      if (metricsCollector != null) {
        metricsCollector.recordAgentExecution(
            agent.agentName(), capability.name(), "SUCCESS", durationNanos);
      }

      log.info(
          "AgentGateway completed execution for agent='{}', capability={}",
          agent.agentName(),
          capability);
      return response;
    } catch (Exception ex) {
      long durationNanos = System.nanoTime() - startNanos;
      if (metricsCollector != null) {
        metricsCollector.recordAgentExecution(
            agent.agentName(), capability.name(), "FAILURE", durationNanos);
      }
      log.error(
          "AgentGateway execution failed for agent='{}', capability={}: {}",
          agent.agentName(),
          capability,
          ex.getMessage());
      throw ex;
    }
  }
}
