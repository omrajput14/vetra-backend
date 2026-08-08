package app.vetra.ai.agent.impl;

import app.vetra.ai.agent.AIAgent;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentHealth;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.config.AgentProperties;
import app.vetra.ai.gateway.AIGateway;
import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Specialized AI agent responsible for veterinary disease knowledge retrieval and clinical
 * explanations. (Designed for future RAG knowledge base extension).
 */
@Component
public class KnowledgeAgent implements AIAgent {

  private static final Logger log = LoggerFactory.getLogger(KnowledgeAgent.class);
  public static final String AGENT_NAME = "KnowledgeAgent";

  private final AIGateway aiGateway;
  private final AgentProperties agentProperties;

  /**
   * Constructs KnowledgeAgent.
   *
   * @param aiGateway underlying AI gateway
   * @param agentProperties agent configuration properties
   */
  public KnowledgeAgent(AIGateway aiGateway, AgentProperties agentProperties) {
    this.aiGateway = aiGateway;
    this.agentProperties = agentProperties;
  }

  @Override
  public AgentResponse execute(AgentRequest request) {
    String promptId =
        agentProperties != null
            ? agentProperties.getKnowledgePromptId()
            : "knowledge.disease.v1";

    log.info("KnowledgeAgent executing promptId={} for request={}", promptId, request.capability());

    AIRequest aiRequest =
        new AIRequest(
            promptId,
            request.inputVariables() != null ? request.inputVariables() : Map.of(),
            request.imageUrl(),
            request.cacheBypass(),
            Set.of(AICapability.JSON_MODE),
            null);

    AIResponse rawResponse = aiGateway.execute(aiRequest, request.executionContext());

    return new AgentResponse(
        rawResponse,
        agentName(),
        request.capability(),
        request.metadata() != null ? request.metadata() : Map.of());
  }

  @Override
  public String agentName() {
    return AGENT_NAME;
  }

  @Override
  public Set<AgentCapability> supportedCapabilities() {
    return Set.of(AgentCapability.KNOWLEDGE);
  }

  @Override
  public AgentHealth healthStatus() {
    return AgentHealth.HEALTHY;
  }
}
