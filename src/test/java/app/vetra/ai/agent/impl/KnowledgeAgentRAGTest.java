package app.vetra.ai.agent.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.config.AgentProperties;
import app.vetra.ai.gateway.AIGateway;
import app.vetra.ai.model.AIExecutionContext;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.retrieval.KnowledgeRetriever;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeAgentRAGTest {

  private AIGateway aiGateway;
  private KnowledgeRetriever knowledgeRetriever;
  private KnowledgeAgent knowledgeAgent;

  @BeforeEach
  void setUp() {
    aiGateway = mock(AIGateway.class);
    knowledgeRetriever = mock(KnowledgeRetriever.class);
    AgentProperties agentProperties = new AgentProperties();
    knowledgeAgent = new KnowledgeAgent(aiGateway, agentProperties, knowledgeRetriever);
  }

  @Test
  void testExecuteWithRAG_retrievesContextAndInjectsIntoPrompt() {
    RetrievedContext mockContext =
        new RetrievedContext(
            "[Source: WOAH] Foot and mouth virus etiology details",
            List.of(new Citation("FMD Handbook", "c-1", "WOAH", 0.88)),
            1,
            30,
            0.88);

    when(knowledgeRetriever.retrieveContext(any(), any(Integer.class), any(Double.class), any(SearchFilter.class)))
        .thenReturn(mockContext);

    AIResponse mockAiResponse =
        new AIResponse(
            "{\"disease\":\"Foot and Mouth\",\"etiology\":\"Viral\"}",
            "knowledge.disease.v1",
            "gemini",
            "gemini-1.5-flash",
            15,
            25,
            "stop");

    when(aiGateway.execute(any(AIRequest.class), any(AIExecutionContext.class)))
        .thenReturn(mockAiResponse);

    AgentRequest request =
        AgentRequest.of(
            AgentCapability.KNOWLEDGE,
            Map.of("diseaseName", "Foot and Mouth", "species", "CATTLE"),
            AIExecutionContext.of("tenant-1", "user-1"));

    AgentResponse response = knowledgeAgent.execute(request);

    assertNotNull(response);
    assertEquals("KnowledgeAgent", response.agentName());
    assertEquals(AgentCapability.KNOWLEDGE, response.capability());
    assertEquals(1, response.metadata().get("retrievedChunks"));
    assertNotNull(response.metadata().get("avgSimilarity"));

    // Verify AIRequest was passed the retrieved context
    verify(aiGateway).execute(
        argThat(aiReq -> aiReq.variables().containsKey("retrievedContext")
            && aiReq.variables().get("retrievedContext").toString().contains("Foot and mouth virus etiology")),
        any(AIExecutionContext.class));
  }
}
