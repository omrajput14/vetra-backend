package app.vetra.ai.dto.advisor;

import app.vetra.ai.entity.AIAdvisorRiskLevel;
import java.util.List;

/** Structured clinical assessment output produced by the AI Veterinary Advisor. */
public record AIAdvisorAssessmentDTO(
    List<PossibleConditionDTO> possibleConditions,
    List<String> userReportedSymptoms,
    List<String> keyObservations,
    AIAdvisorRiskLevel riskLevel,
    boolean requiresVeterinarianReview,
    String recommendedNextStep,
    String disclaimer
) {

  /** Backwards-compatible constructor when userReportedSymptoms is omitted. */
  public AIAdvisorAssessmentDTO(
      List<PossibleConditionDTO> possibleConditions,
      List<String> keyObservations,
      AIAdvisorRiskLevel riskLevel,
      boolean requiresVeterinarianReview,
      String recommendedNextStep,
      String disclaimer) {
    this(
        possibleConditions,
        List.of(),
        keyObservations,
        riskLevel,
        requiresVeterinarianReview,
        recommendedNextStep,
        disclaimer);
  }
}
