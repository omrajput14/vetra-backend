package app.vetra.ai.workflow.clinical;

import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.workflow.clinical.model.ClinicalDiagnosisReport;
import app.vetra.ai.workflow.clinical.model.ClinicalWorkflowContext;
import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TreatmentPlan;
import app.vetra.ai.workflow.clinical.model.WorkflowStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Pure report assembly component that synthesizes intermediate outputs from the
 * ClinicalWorkflowContext into an immutable, veterinarian-ready ClinicalDiagnosisReport.
 *
 * <p>Does not invoke agents or contain orchestration business logic.
 */
@Component
public class ClinicalReportBuilder {

  /**
   * Assembles a structured ClinicalDiagnosisReport from the provided workflow context.
   *
   * @param context shared clinical workflow context
   * @return comprehensive {@link ClinicalDiagnosisReport}
   */
  public ClinicalDiagnosisReport buildReport(ClinicalWorkflowContext context) {
    if (context == null) {
      throw new IllegalArgumentException("ClinicalWorkflowContext cannot be null");
    }

    UUID reportId = UUID.randomUUID();
    UUID scanId = context.getRequest().scanId();
    UUID animalId = context.getRequest().animalId();

    String animalInfo =
        String.format(
            "Species: %s | Breed: %s | UploadedBy: %s",
            context.getRequest().species(),
            context.getRequest().breed(),
            context.getRequest().uploadedBy());

    List<String> symptoms = context.getRequest().symptoms();
    List<DiseaseCandidate> candidates =
        context.getRankedDiseases() != null ? context.getRankedDiseases() : List.of();

    DiseaseCandidate topCandidate =
        !candidates.isEmpty()
            ? candidates.get(0)
            : new DiseaseCandidate(
                "Unspecified Observation",
                BigDecimal.valueOf(0.10),
                "No definitive condition identified",
                List.of(),
                true);

    String primaryDiagnosis = topCandidate.diseaseName();
    BigDecimal confidenceScore = topCandidate.confidence();
    String supportingEvidence = topCandidate.evidence();

    String literature =
        (context.getRetrievedContext() != null && context.getRetrievedContext().contextText() != null)
            ? context.getRetrievedContext().contextText()
            : "No retrieved literature attached.";

    List<Citation> citations =
        (context.getRetrievedContext() != null && context.getRetrievedContext().citations() != null)
            ? context.getRetrievedContext().citations()
            : topCandidate.citations();

    TreatmentPlan treatmentPlan =
        context.getTreatmentPlan() != null
            ? context.getTreatmentPlan()
            : TreatmentPlan.defaultPlan(primaryDiagnosis);

    String treatmentRecommendation =
        treatmentPlan.primaryTreatment()
            + (treatmentPlan.medications().isEmpty()
                ? ""
                : "\nMedications: " + String.join(", ", treatmentPlan.medications()));

    List<String> immediateActions = assembleImmediateActions(context, treatmentPlan, topCandidate);
    List<String> monitoringAdvice = new ArrayList<>(treatmentPlan.monitoringAdvice());
    Map<String, Object> summary = assembleExecutionSummary(context, candidates);

    long durationMs = (System.nanoTime() - context.getStartTimeNanos()) / 1_000_000L;
    WorkflowStatus finalStatus = context.getStatus() != null ? context.getStatus() : WorkflowStatus.SUCCESS;

    return new ClinicalDiagnosisReport(
        reportId,
        scanId,
        animalId,
        animalInfo,
        symptoms,
        candidates,
        primaryDiagnosis,
        confidenceScore,
        supportingEvidence,
        literature,
        treatmentRecommendation,
        immediateActions,
        monitoringAdvice,
        citations,
        Instant.now(),
        summary,
        durationMs,
        finalStatus);
  }

  private List<String> assembleImmediateActions(
      ClinicalWorkflowContext context, TreatmentPlan treatmentPlan, DiseaseCandidate topCandidate) {

    List<String> immediateActions = new ArrayList<>(treatmentPlan.precautions());
    if (context.getTriageAssessment() != null) {
      TriageAssessment triage = context.getTriageAssessment();
      String triageHeader = "CLINICAL TRIAGE URGENCY: " + triage.urgency();
      if (!immediateActions.contains(triageHeader)) {
        immediateActions.add(0, triageHeader);
      }
      for (String action : triage.recommendedActions()) {
        if (!immediateActions.contains(action)) {
          immediateActions.add(action);
        }
      }
    } else if (topCandidate.requiresUrgentReview() && !immediateActions.contains("Requires urgent veterinarian review")) {
      immediateActions.add(0, "Requires urgent veterinarian review");
    }
    return immediateActions;
  }

  private Map<String, Object> assembleExecutionSummary(
      ClinicalWorkflowContext context, List<DiseaseCandidate> candidates) {

    Map<String, Object> summary = new HashMap<>();
    summary.put("stepTimings", context.getStepTimings());
    summary.put("stepStatuses", context.getStepStatuses());
    summary.put("totalCandidatesRanked", candidates.size());
    summary.put("hasGroundedLiterature", context.getRetrievedContext() != null && context.getRetrievedContext().totalChunks() > 0);

    if (context.getTriageAssessment() != null) {
      TriageAssessment triage = context.getTriageAssessment();
      summary.put("triageUrgency", triage.urgency().name());
      summary.put("triageConfidence", triage.confidence());
      summary.put("triageRationale", triage.rationale());
      summary.put("triageWarningSigns", triage.warningSigns());
      summary.put("triageImmediateReviewRequired", triage.requiresImmediateVeterinaryReview());
    }

    if (!context.getErrors().isEmpty()) {
      summary.put("errorsEncountered", context.getErrors());
    }
    return summary;
  }
}
