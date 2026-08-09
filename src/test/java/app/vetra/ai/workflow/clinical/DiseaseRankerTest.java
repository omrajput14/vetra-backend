package app.vetra.ai.workflow.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiseaseRankerTest {

  private DiseaseRanker ranker;

  @BeforeEach
  void setUp() {
    ranker = new DiseaseRanker();
  }

  @Test
  void testRankDiseases_withStructuredDiagnosisAndCitations() {
    String jsonContent =
        """
        ```json
        {
          "condition": "Bovine Foot and Mouth Disease",
          "confidence": 0.94,
          "observations": ["Oral vesicles", "Bovine Foot and Mouth Disease", "Lameness"],
          "requiresVeterinarianReview": true
        }
        ```
        """;

    AIResponse raw = new AIResponse(jsonContent, "1.0", "gemini", "gemini-1.5-pro", 40, 90, "STOP");
    AgentResponse diagnosisResponse =
        new AgentResponse(raw, "DiagnosisAgent", AgentCapability.DIAGNOSIS, Map.of());

    List<Citation> citations =
        List.of(new Citation("WOAH Terrestrial Manual", "FMD-01", "WOAH", 0.95));
    RetrievedContext context =
        new RetrievedContext("FMD is a contagious viral disease.", citations, 1, 25, 0.95);

    List<DiseaseCandidate> candidates =
        ranker.rankDiseases(diagnosisResponse, context, List.of("Blisters", "Drooling"));

    assertNotNull(candidates);
    assertFalse(candidates.isEmpty());

    // Top candidate should be the primary condition with highest confidence
    DiseaseCandidate top = candidates.get(0);
    assertEquals("Bovine Foot and Mouth Disease", top.diseaseName());
    assertEquals(new BigDecimal("0.94"), top.confidence());
    assertTrue(top.requiresUrgentReview());
    assertEquals(1, top.citations().size());

    // Verify deduplication: "Bovine Foot and Mouth Disease" is not duplicated in observations
    long fmdCount =
        candidates.stream()
            .filter(c -> c.diseaseName().equalsIgnoreCase("Bovine Foot and Mouth Disease"))
            .count();
    assertEquals(1, fmdCount);
  }

  @Test
  void testRankDiseases_withFallbackWhenDiagnosisEmpty() {
    RetrievedContext context =
        new RetrievedContext("General bovine health.", List.of(), 0, 10, 0.50);

    List<DiseaseCandidate> candidates =
        ranker.rankDiseases(null, context, List.of("Fever", "Anorexia"));

    assertNotNull(candidates);
    assertEquals(1, candidates.size());
    assertTrue(candidates.get(0).diseaseName().contains("Clinical Syndrome"));
    assertEquals(0, BigDecimal.valueOf(0.50).compareTo(candidates.get(0).confidence()));
  }

  @Test
  void testNormalizeConfidence() {
    assertEquals(new BigDecimal("0.85"), ranker.normalizeConfidence(BigDecimal.valueOf(0.854)));
    assertEquals(new BigDecimal("1.00"), ranker.normalizeConfidence(BigDecimal.valueOf(1.50)));
    assertEquals(new BigDecimal("0.00"), ranker.normalizeConfidence(BigDecimal.valueOf(-0.20)));
    assertEquals(new BigDecimal("0.50"), ranker.normalizeConfidence(null));
  }
}
