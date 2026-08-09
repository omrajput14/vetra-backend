package app.vetra.ai.workflow.clinical.clinicalcase.repository;

import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalCaseTimeline;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEvent;
import app.vetra.ai.workflow.clinical.model.action.ClinicalActionPlan;
import app.vetra.ai.workflow.clinical.model.explainability.ClinicalDecisionSupport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Longitudinal clinical case repository interface.
 *
 * <p>Encounters and timeline events are append-only and immutable.
 */
public interface ClinicalCaseRepository {

  ClinicalCase createCase(ClinicalCase clinicalCase);

  Optional<ClinicalCase> findById(UUID caseId);

  List<ClinicalCase> findByAnimalId(UUID animalId);

  List<ClinicalCase> findAllCases();

  ClinicalCase updateCaseStatus(UUID caseId, ClinicalCaseStatus newStatus);

  ClinicalEncounter saveEncounter(ClinicalEncounter encounter);

  List<ClinicalEncounter> findEncountersByCaseId(UUID caseId);

  ClinicalTimelineEvent appendTimelineEvent(ClinicalTimelineEvent event);

  ClinicalCaseTimeline getTimeline(UUID caseId);

  ClinicalFollowUp saveFollowUp(ClinicalFollowUp followUp);

  List<ClinicalFollowUp> findFollowUpsByCaseId(UUID caseId);

  ClinicalFollowUp updateFollowUpStatus(UUID followUpId, FollowUpStatus newStatus);

  Optional<ClinicalDecisionSupport> findLatestDecisionSupportByCaseId(UUID caseId);

  Optional<ClinicalActionPlan> findLatestActionPlanByCaseId(UUID caseId);

  Optional<TreatmentResponse> findLatestTreatmentResponseByCaseId(UUID caseId);
}
