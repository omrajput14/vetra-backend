package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.evidence.MultiModalEvidenceSummary;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Veterinarian-ready complete Clinical Diagnosis Report produced by the ClinicalReportBuilder.
 *
 * @param reportId unique report identifier
 * @param scanId linked diagnostic scan identifier
 * @param animalId target animal identifier
 * @param animalInformation animal identity and background
 * @param symptoms observed clinical symptoms
 * @param mostLikelyDiseases ranked disease candidates
 * @param primaryDiagnosis name of the highest-ranked disease
 * @param confidenceScore top normalized confidence score
 * @param supportingEvidence clinical reasoning and evidence
 * @param retrievedLiterature summary of literature references from RAG
 * @param treatmentRecommendation synthesized treatment protocols
 * @param immediateActions immediate clinical/biosecurity actions
 * @param monitoringAdvice ongoing observation and care advice
 * @param references grounded literature citations
 * @param evidenceSummary multi-modal evidence summary breakdown (optional)
 * @param decisionSupport decision support, traceability, and review flag (optional)
 * @param actionPlan operational care plan and prioritized actions (optional)
 * @param timestamp creation timestamp
 * @param agentExecutionSummary summary of agents involved and performance
 * @param totalDurationMs total workflow latency in milliseconds
 * @param status workflow status
 */
public record ClinicalDiagnosisReport(
    UUID reportId,
    UUID scanId,
    UUID animalId,
    String animalInformation,
    List<String> symptoms,
    List<DiseaseCandidate> mostLikelyDiseases,
    String primaryDiagnosis,
    BigDecimal confidenceScore,
    String supportingEvidence,
    String retrievedLiterature,
    String treatmentRecommendation,
    List<String> immediateActions,
    List<String> monitoringAdvice,
    List<Citation> references,
    MultiModalEvidenceSummary evidenceSummary,
    ClinicalDecisionSupport decisionSupport,
    ClinicalActionPlan actionPlan,
    Instant timestamp,
    Map<String, Object> agentExecutionSummary,
    long totalDurationMs,
    WorkflowStatus status) {

  /** Backward-compatible 18-argument constructor. */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public ClinicalDiagnosisReport(
      UUID reportId,
      UUID scanId,
      UUID animalId,
      String animalInformation,
      List<String> symptoms,
      List<DiseaseCandidate> mostLikelyDiseases,
      String primaryDiagnosis,
      BigDecimal confidenceScore,
      String supportingEvidence,
      String retrievedLiterature,
      String treatmentRecommendation,
      List<String> immediateActions,
      List<String> monitoringAdvice,
      List<Citation> references,
      Instant timestamp,
      Map<String, Object> agentExecutionSummary,
      long totalDurationMs,
      WorkflowStatus status) {
    this(
        reportId,
        scanId,
        animalId,
        animalInformation,
        symptoms,
        mostLikelyDiseases,
        primaryDiagnosis,
        confidenceScore,
        supportingEvidence,
        retrievedLiterature,
        treatmentRecommendation,
        immediateActions,
        monitoringAdvice,
        references,
        null,
        null,
        null,
        timestamp,
        agentExecutionSummary,
        totalDurationMs,
        status);
  }

  /** Backward-compatible 19-argument constructor. */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public ClinicalDiagnosisReport(
      UUID reportId,
      UUID scanId,
      UUID animalId,
      String animalInformation,
      List<String> symptoms,
      List<DiseaseCandidate> mostLikelyDiseases,
      String primaryDiagnosis,
      BigDecimal confidenceScore,
      String supportingEvidence,
      String retrievedLiterature,
      String treatmentRecommendation,
      List<String> immediateActions,
      List<String> monitoringAdvice,
      List<Citation> references,
      MultiModalEvidenceSummary evidenceSummary,
      Instant timestamp,
      Map<String, Object> agentExecutionSummary,
      long totalDurationMs,
      WorkflowStatus status) {
    this(
        reportId,
        scanId,
        animalId,
        animalInformation,
        symptoms,
        mostLikelyDiseases,
        primaryDiagnosis,
        confidenceScore,
        supportingEvidence,
        retrievedLiterature,
        treatmentRecommendation,
        immediateActions,
        monitoringAdvice,
        references,
        evidenceSummary,
        null,
        null,
        timestamp,
        agentExecutionSummary,
        totalDurationMs,
        status);
  }

  /** Backward-compatible 20-argument constructor (with decisionSupport, without actionPlan). */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public ClinicalDiagnosisReport(
      UUID reportId,
      UUID scanId,
      UUID animalId,
      String animalInformation,
      List<String> symptoms,
      List<DiseaseCandidate> mostLikelyDiseases,
      String primaryDiagnosis,
      BigDecimal confidenceScore,
      String supportingEvidence,
      String retrievedLiterature,
      String treatmentRecommendation,
      List<String> immediateActions,
      List<String> monitoringAdvice,
      List<Citation> references,
      MultiModalEvidenceSummary evidenceSummary,
      ClinicalDecisionSupport decisionSupport,
      Instant timestamp,
      Map<String, Object> agentExecutionSummary,
      long totalDurationMs,
      WorkflowStatus status) {
    this(
        reportId,
        scanId,
        animalId,
        animalInformation,
        symptoms,
        mostLikelyDiseases,
        primaryDiagnosis,
        confidenceScore,
        supportingEvidence,
        retrievedLiterature,
        treatmentRecommendation,
        immediateActions,
        monitoringAdvice,
        references,
        evidenceSummary,
        decisionSupport,
        null,
        timestamp,
        agentExecutionSummary,
        totalDurationMs,
        status);
  }

  /** Canonical constructor with non-null defaults. */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public ClinicalDiagnosisReport {
    reportId = reportId != null ? reportId : UUID.randomUUID();
    symptoms = symptoms != null ? List.copyOf(symptoms) : List.of();
    mostLikelyDiseases = mostLikelyDiseases != null ? List.copyOf(mostLikelyDiseases) : List.of();
    primaryDiagnosis = primaryDiagnosis != null ? primaryDiagnosis.trim() : "Unspecified Observation";
    confidenceScore = confidenceScore != null ? confidenceScore : BigDecimal.valueOf(0.10);
    supportingEvidence = supportingEvidence != null ? supportingEvidence.trim() : "";
    retrievedLiterature = retrievedLiterature != null ? retrievedLiterature.trim() : "";
    treatmentRecommendation = treatmentRecommendation != null ? treatmentRecommendation.trim() : "";
    immediateActions = immediateActions != null ? List.copyOf(immediateActions) : List.of();
    monitoringAdvice = monitoringAdvice != null ? List.copyOf(monitoringAdvice) : List.of();
    references = references != null ? List.copyOf(references) : List.of();
    timestamp = timestamp != null ? timestamp : Instant.now();
    agentExecutionSummary = agentExecutionSummary != null ? Map.copyOf(agentExecutionSummary) : Map.of();
    status = status != null ? status : WorkflowStatus.SUCCESS;
  }
}
