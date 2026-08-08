package app.vetra.ai.workflow.clinical;

import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceType;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Multi-modal disease ranking engine. Merges diagnostic outputs, literature, laboratory,
 * vital signs, and symptoms, normalizing active modality weights dynamically when evidence is missing.
 *
 * <p>Engineering default weights (configurable via application properties):
 * <ul>
 *   <li>Vision Weight: 0.35</li>
 *   <li>Lab Weight: 0.25</li>
 *   <li>Vital Weight: 0.15</li>
 *   <li>Symptom Weight: 0.15</li>
 *   <li>RAG Weight: 0.10</li>
 * </ul>
 */
@Component
public class DiseaseRanker {

  private static final Logger log = LoggerFactory.getLogger(DiseaseRanker.class);
  private final ObjectMapper objectMapper;

  private final double visionWeight;
  private final double labWeight;
  private final double vitalWeight;
  private final double symptomWeight;
  private final double ragWeight;

  public DiseaseRanker() {
    this(0.35, 0.25, 0.15, 0.15, 0.10);
  }

  public DiseaseRanker(
      @Value("${vetra.ai.ranking.weights.vision:0.35}") double visionWeight,
      @Value("${vetra.ai.ranking.weights.lab:0.25}") double labWeight,
      @Value("${vetra.ai.ranking.weights.vital:0.15}") double vitalWeight,
      @Value("${vetra.ai.ranking.weights.symptom:0.15}") double symptomWeight,
      @Value("${vetra.ai.ranking.weights.rag:0.10}") double ragWeight) {

    this.objectMapper = new ObjectMapper();
    this.visionWeight = validateWeight(visionWeight, 0.35);
    this.labWeight = validateWeight(labWeight, 0.25);
    this.vitalWeight = validateWeight(vitalWeight, 0.15);
    this.symptomWeight = validateWeight(symptomWeight, 0.15);
    this.ragWeight = validateWeight(ragWeight, 0.10);
  }

  private double validateWeight(double weight, double defaultValue) {
    if (weight < 0.0 || weight > 1.0) {
      log.warn("Invalid weight configured: {}. Falling back to default: {}", weight, defaultValue);
      return defaultValue;
    }
    return weight;
  }

  /**
   * Ranks candidate diseases taking into account all available multi-modal evidence.
   *
   * @param diagnosisResponse visual diagnosis output
   * @param retrievedContext grounded context from KnowledgeAgent
   * @param symptoms symptoms list
   * @param unifiedEvidence unified evidence collection (optional)
   * @return sorted list of {@link DiseaseCandidate}
   */
  public List<DiseaseCandidate> rankDiseases(
      AgentResponse diagnosisResponse,
      RetrievedContext retrievedContext,
      List<String> symptoms,
      UnifiedClinicalEvidence unifiedEvidence) {

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

    // Apply dynamic weight normalization across active modalities for each candidate
    List<DiseaseCandidate> weightedCandidates = new ArrayList<>();
    for (DiseaseCandidate candidate : rawCandidates) {
      BigDecimal score = calculateWeightedScore(candidate, symptoms, retrievedContext, unifiedEvidence);
      weightedCandidates.add(
          new DiseaseCandidate(
              candidate.diseaseName(),
              normalizeConfidence(score),
              candidate.evidence(),
              candidate.citations(),
              candidate.requiresUrgentReview()));
    }

    return deduplicateAndSort(weightedCandidates);
  }

  /** Overloaded method maintaining backward compatibility. */
  public List<DiseaseCandidate> rankDiseases(
      AgentResponse diagnosisResponse, RetrievedContext retrievedContext, List<String> symptoms) {
    return rankDiseases(diagnosisResponse, retrievedContext, symptoms, null);
  }

  private BigDecimal calculateWeightedScore(
      DiseaseCandidate candidate,
      List<String> symptoms,
      RetrievedContext retrievedContext,
      UnifiedClinicalEvidence unifiedEvidence) {

    if (unifiedEvidence == null) {
      return candidate.confidence();
    }
    double activeWeightSum = 0.0;
    double weightedScoreSum = 0.0;

    // 1. Vision modality
    double visionScore = candidate.confidence().doubleValue();
    activeWeightSum += visionWeight;
    weightedScoreSum += visionScore * visionWeight;

    // 2. Symptoms modality
    if (symptoms != null && !symptoms.isEmpty()) {
      double symptomScore = 0.70;
      activeWeightSum += symptomWeight;
      weightedScoreSum += symptomScore * symptomWeight;
    }

    // 3. Lab modality
    if (unifiedEvidence != null && !unifiedEvidence.findByType(EvidenceType.LAB_RESULT).isEmpty()) {
      List<ClinicalEvidence> labs = unifiedEvidence.findByType(EvidenceType.LAB_RESULT);
      double labScore = calculateModalityScoreForCandidate(candidate.diseaseName(), labs);
      activeWeightSum += labWeight;
      weightedScoreSum += labScore * labWeight;
    }

    // 4. Vital modality
    if (unifiedEvidence != null && !unifiedEvidence.findByType(EvidenceType.VITAL_SIGN).isEmpty()) {
      List<ClinicalEvidence> vitals = unifiedEvidence.findByType(EvidenceType.VITAL_SIGN);
      double vitalScore = calculateModalityScoreForCandidate(candidate.diseaseName(), vitals);
      activeWeightSum += vitalWeight;
      weightedScoreSum += vitalScore * vitalWeight;
    }

    // 5. RAG literature modality
    if (retrievedContext != null && retrievedContext.totalChunks() > 0) {
      double rScore = retrievedContext.avgSimilarityScore() > 0 ? retrievedContext.avgSimilarityScore() : 0.80;
      activeWeightSum += ragWeight;
      weightedScoreSum += rScore * ragWeight;
    }

    if (activeWeightSum <= 0.0) {
      return candidate.confidence();
    }

    // Normalize by active weights sum so missing modalities do not depress score
    double finalScore = weightedScoreSum / activeWeightSum;
    return BigDecimal.valueOf(finalScore);
  }

  private double calculateModalityScoreForCandidate(String diseaseName, List<ClinicalEvidence> evidenceList) {
    String term = diseaseName.toLowerCase();
    boolean matches = false;
    for (ClinicalEvidence e : evidenceList) {
      if (e.summary().toLowerCase().contains(term) || e.observations().stream().anyMatch(o -> o.toLowerCase().contains(term))) {
        matches = true;
        break;
      }
    }
    return matches ? 0.85 : 0.50;
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
