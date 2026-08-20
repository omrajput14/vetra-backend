package app.vetra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for AI agents in the Vetra platform. Binds from {@code vetra.ai.agents}.
 */
@ConfigurationProperties(prefix = "vetra.ai.agents")
public final class AgentProperties {

  private final String diagnosisPromptId;
  private final String treatmentPromptId;
  private final String knowledgePromptId;
  private final String reportPromptId;
  private final String triagePromptId;
  private final String advisorPromptId;

  /**
   * Spring constructor binding.
   *
   * @param diagnosisPromptId prompt ID for diagnosis agent
   * @param treatmentPromptId prompt ID for treatment agent
   * @param knowledgePromptId prompt ID for knowledge agent
   * @param reportPromptId prompt ID for report agent
   * @param triagePromptId prompt ID for triage agent
   * @param advisorPromptId prompt ID for advisor agent
   */
  public AgentProperties(
      @DefaultValue("diagnosis.visual.v1") String diagnosisPromptId,
      @DefaultValue("treatment.recommendation.v1") String treatmentPromptId,
      @DefaultValue("knowledge.disease.v1") String knowledgePromptId,
      @DefaultValue("report.summary.v1") String reportPromptId,
      @DefaultValue("triage.assessment.v1") String triagePromptId,
      @DefaultValue("diagnosis.advisor.v1") String advisorPromptId) {
    this.diagnosisPromptId = diagnosisPromptId;
    this.treatmentPromptId = treatmentPromptId;
    this.knowledgePromptId = knowledgePromptId;
    this.reportPromptId = reportPromptId;
    this.triagePromptId = triagePromptId;
    this.advisorPromptId = advisorPromptId;
  }

  /** Default constructor for fallback scenarios. */
  public AgentProperties() {
    this(
        "diagnosis.visual.v1",
        "treatment.recommendation.v1",
        "knowledge.disease.v1",
        "report.summary.v1",
        "triage.assessment.v1",
        "diagnosis.advisor.v1");
  }

  /** Returns diagnosis prompt ID. */
  public String getDiagnosisPromptId() {
    return diagnosisPromptId;
  }

  /** Returns treatment prompt ID. */
  public String getTreatmentPromptId() {
    return treatmentPromptId;
  }

  /** Returns knowledge prompt ID. */
  public String getKnowledgePromptId() {
    return knowledgePromptId;
  }

  /** Returns report prompt ID. */
  public String getReportPromptId() {
    return reportPromptId;
  }

  /** Returns triage prompt ID. */
  public String getTriagePromptId() {
    return triagePromptId;
  }

  /** Returns advisor prompt ID. */
  public String getAdvisorPromptId() {
    return advisorPromptId;
  }
}
