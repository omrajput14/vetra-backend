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
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.retrieval.KnowledgeRetriever;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Specialized AI agent responsible for veterinary disease knowledge retrieval, clinical literature
 * explanations, and evidence-grounded answers using the Veterinary Knowledge Platform (RAG).
 */
@Component
public class KnowledgeAgent implements AIAgent {

  private static final Logger log = LoggerFactory.getLogger(KnowledgeAgent.class);
  public static final String AGENT_NAME = "KnowledgeAgent";

  private final AIGateway aiGateway;
  private final AgentProperties agentProperties;
  private final KnowledgeRetriever knowledgeRetriever;

  /**
   * Constructs KnowledgeAgent with default retriever.
   *
   * @param aiGateway underlying AI gateway
   * @param agentProperties agent configuration properties
   */
  public KnowledgeAgent(AIGateway aiGateway, AgentProperties agentProperties) {
    this(aiGateway, agentProperties, null);
  }

  /**
   * Constructs KnowledgeAgent with full dependencies.
   *
   * @param aiGateway underlying AI gateway
   * @param agentProperties agent configuration properties
   * @param knowledgeRetriever veterinary RAG retriever (optional)
   */
  @Autowired
  public KnowledgeAgent(
      AIGateway aiGateway,
      AgentProperties agentProperties,
      @Autowired(required = false) KnowledgeRetriever knowledgeRetriever) {
    this.aiGateway = aiGateway;
    this.agentProperties = agentProperties;
    this.knowledgeRetriever = knowledgeRetriever;
  }

  @Override
  public AgentResponse execute(AgentRequest request) {
    String promptId =
        agentProperties != null
            ? agentProperties.getKnowledgePromptId()
            : "knowledge.disease.v1";

    log.info("KnowledgeAgent executing promptId={} for request={}", promptId, request.capability());

    Map<String, Object> variables = new HashMap<>();
    if (request.inputVariables() != null) {
      variables.putAll(request.inputVariables());
    }

    // Determine search query for RAG
    String query = extractQuery(variables);
    SearchFilter filter = extractFilter(variables);

    RetrievedContext retrievedContext = RetrievedContext.empty();
    if (knowledgeRetriever != null && query != null && !query.isBlank()) {
      retrievedContext = knowledgeRetriever.retrieveContext(query, 3, 0.60, filter);
    }

    // Inject retrieved context for grounded prompt rendering
    if (retrievedContext.hasContext()) {
      variables.put("retrievedContext", retrievedContext.contextText());
    } else {
      variables.putIfAbsent("retrievedContext", "No specific clinical literature found in local database.");
    }

    AIRequest aiRequest =
        new AIRequest(
            promptId,
            variables,
            request.imageUrl(),
            request.cacheBypass(),
            Set.of(AICapability.JSON_MODE),
            null);

    AIResponse rawResponse = aiGateway.execute(aiRequest, request.executionContext());

    Map<String, Object> responseMetadata = new HashMap<>();
    if (request.metadata() != null) {
      responseMetadata.putAll(request.metadata());
    }
    responseMetadata.put("retrievedChunks", retrievedContext.totalChunks());
    responseMetadata.put(
        "avgSimilarity",
        String.format(java.util.Locale.ROOT, "%.3f", retrievedContext.avgSimilarityScore()));

    return new AgentResponse(
        rawResponse,
        agentName(),
        request.capability(),
        responseMetadata);
  }

  private String extractQuery(Map<String, Object> variables) {
    if (variables.containsKey("query")) {
      return String.valueOf(variables.get("query"));
    }
    if (variables.containsKey("question")) {
      return String.valueOf(variables.get("question"));
    }
    if (variables.containsKey("diseaseName")) {
      return String.valueOf(variables.get("diseaseName"));
    }
    return null;
  }

  private SearchFilter extractFilter(Map<String, Object> variables) {
    String species =
        variables.containsKey("species") ? String.valueOf(variables.get("species")) : null;
    String category =
        variables.containsKey("category") ? String.valueOf(variables.get("category")) : null;
    return new SearchFilter(species, category, null, null, Map.of());
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
