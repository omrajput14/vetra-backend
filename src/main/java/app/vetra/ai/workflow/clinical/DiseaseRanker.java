package app.vetra.ai.workflow.clinical;

import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Merges diagnostic outputs with literature evidence, eliminates duplicate conditions,
 * normalizes confidence scores to [0.00, 1.00], and produces sorted DiseaseCandidate rankings.
 */
@Component
public class DiseaseRanker {

  private static final Logger log = LoggerFactory.getLogger(DiseaseRanker.class);
  private final ObjectMapper objectMapper;

  public DiseaseRanker() {
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Ranks and deduplicates candidate diseases based on diagnosis observations and retrieved evidence.
   *
   * @param diagnosisResponse output from DiagnosisAgent
   * @param retrievedContext grounded context and citations from KnowledgeAgent (RAG)
   * @param symptoms observed clinical symptoms
   * @return sorted list of {@link DiseaseCandidate} in descending confidence order
   */
  public List<DiseaseCandidate> rankDiseases(
      AgentResponse diagnosisResponse, RetrievedContext retrievedContext, List<String> symptoms) {

    List<DiseaseCandidate> rawCandidates = new ArrayList<>();
    List<Citation> citations =
        retrievedContext != null && retrievedContext.citations() != null
            ? retrievedContext.citations()
            : List.of();

    String evidenceText =
        retrievedContext != null && retrievedContext.contextText() != null
            ? retrievedContext.contextText()
            : "Clinical literature corroboration.";

    if (diagnosisResponse != null && diagnosisResponse.rawResponse() != null) {
      extractFromDiagnosis(diagnosisResponse.rawResponse().content(), citations, evidenceText, rawCandidates);
    }

    // If no candidate was extracted from raw response, synthesize from symptoms
    if (rawCandidates.isEmpty()) {
      String fallbackCondition =
          (symptoms != null && !symptoms.isEmpty())
              ? "Clinical Syndrome: " + String.join(", ", symptoms)
              : "Unspecified Observation";
      rawCandidates.add(
          new DiseaseCandidate(
              fallbackCondition,
              BigDecimal.valueOf(0.50),
              evidenceText,
              citations,
              true));
    }

    return deduplicateAndSort(rawCandidates);
  }

  private void extractFromDiagnosis(
      String content, List<Citation> citations, String evidenceText, List<DiseaseCandidate> candidates) {
    try {
      String cleanJson = cleanMarkdown(content);
      JsonNode node = objectMapper.readTree(cleanJson);

      String mainCondition =
          node.has("condition") && !node.get("condition").asText().isBlank()
              ? node.get("condition").asText().trim()
              : "Unspecified Clinical Observation";

      BigDecimal confidence =
          node.has("confidence") && !node.get("confidence").isNull()
              ? BigDecimal.valueOf(node.get("confidence").asDouble())
              : BigDecimal.valueOf(0.75);

      boolean urgent =
          node.has("requiresVeterinarianReview") && node.get("requiresVeterinarianReview").asBoolean(true);

      candidates.add(
          new DiseaseCandidate(
              mainCondition,
              normalizeConfidence(confidence),
              evidenceText,
              citations,
              urgent));

      // Extract secondary observations / differential diagnoses if present
      if (node.has("observations") && node.get("observations").isArray()) {
        for (JsonNode obsNode : node.get("observations")) {
          String obsText = obsNode.asText().trim();
          if (!obsText.equalsIgnoreCase(mainCondition) && !obsText.isBlank()) {
            candidates.add(
                new DiseaseCandidate(
                    obsText,
                    normalizeConfidence(confidence.multiply(BigDecimal.valueOf(0.70))),
                    "Secondary visual observation: " + obsText,
                    citations,
                    false));
          }
        }
      }

    } catch (Exception e) {
      log.warn("Failed to parse structured diagnostic JSON during ranking: {}", e.getMessage());
      candidates.add(
          new DiseaseCandidate(
              "Visual Clinical Observation",
              BigDecimal.valueOf(0.50),
              evidenceText,
              citations,
              true));
    }
  }

  /**
   * Normalizes any numerical confidence value into a calibrated [0.00, 1.00] range with 2 decimal places.
   *
   * @param score raw score
   * @return normalized score
   */
  public BigDecimal normalizeConfidence(BigDecimal score) {
    if (score == null) {
      return BigDecimal.valueOf(0.50).setScale(2, RoundingMode.HALF_UP);
    }
    double val = score.doubleValue();
    if (val < 0.0) {
      val = 0.0;
    }
    if (val > 1.0) {
      val = 1.0;
    }
    return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
  }

  private List<DiseaseCandidate> deduplicateAndSort(List<DiseaseCandidate> candidates) {
    Set<String> seen = new HashSet<>();
    List<DiseaseCandidate> unique = new ArrayList<>();

    for (DiseaseCandidate c : candidates) {
      String key = c.diseaseName().toLowerCase().replaceAll("[^a-z0-9]", "");
      if (seen.add(key)) {
        unique.add(c);
      }
    }

    unique.sort(Comparator.comparing(DiseaseCandidate::confidence).reversed());
    return Collections.unmodifiableList(unique);
  }

  private String cleanMarkdown(String text) {
    if (text == null) {
      return "{}";
    }
    String cleaned = text.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring(7);
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring(3);
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3);
    }
    return cleaned.trim();
  }
}
