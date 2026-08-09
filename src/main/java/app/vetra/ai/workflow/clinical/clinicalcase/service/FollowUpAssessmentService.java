package app.vetra.ai.workflow.clinical.clinicalcase.service;

import app.vetra.ai.event.ClinicalConditionWorsenedEvent;
import app.vetra.ai.event.TreatmentResponseRecordedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.clinicalcase.analysis.ClinicalProgressAnalyzer;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.ClinicalFollowUp;
import app.vetra.ai.workflow.clinical.clinicalcase.followup.FollowUpStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCaseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.ClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponseStatus;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEvent;
import app.vetra.ai.workflow.clinical.clinicalcase.timeline.ClinicalTimelineEventType;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service managing treatment response analysis, follow-up state evaluations,
 * and escalation safety triggers following encounter recording.
 */
@Service
public class FollowUpAssessmentService {

  private static final Logger log = LoggerFactory.getLogger(FollowUpAssessmentService.class);

  private final ClinicalCaseRepository repository;
  private final ClinicalProgressAnalyzer progressAnalyzer;
  private final ApplicationEventPublisher eventPublisher;
  private final AIMetricsCollector metricsCollector;

  public FollowUpAssessmentService(
      ClinicalCaseRepository repository,
      ClinicalProgressAnalyzer progressAnalyzer,
      ApplicationEventPublisher eventPublisher,
      AIMetricsCollector metricsCollector) {
    this.repository = repository;
    this.progressAnalyzer = progressAnalyzer;
    this.eventPublisher = eventPublisher;
    this.metricsCollector = metricsCollector;
  }

  public TreatmentResponse evaluateEncounterProgress(UUID caseId, ClinicalEncounter currentEncounter) {
    if (caseId == null || currentEncounter == null) {
      throw new IllegalArgumentException("caseId and currentEncounter cannot be null");
    }

    ClinicalCase clinicalCase = repository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("ClinicalCase not found with id: " + caseId));

    List<ClinicalEncounter> encounters = repository.findEncountersByCaseId(caseId);
    ClinicalEncounter previousEncounter = findPreviousEncounter(encounters, currentEncounter);

    TreatmentResponse response = progressAnalyzer.analyzeProgress(previousEncounter, currentEncounter);

    boolean requiresEscalation = isEscalationRequired(currentEncounter, previousEncounter, response);

    if (requiresEscalation) {
      handleEscalation(clinicalCase, currentEncounter, response);
    } else {
      handleNormalProgress(caseId, currentEncounter, response);
    }

    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new TreatmentResponseRecordedEvent(
              caseId,
              response.previousEncounterId(),
              response.currentEncounterId(),
              response.status(),
              Instant.now()));
    }
    if (metricsCollector != null) {
      metricsCollector.recordTreatmentResponse(response.status().name());
    }

    log.info("FollowUpAssessment completed for caseId={}: responseStatus={}", caseId, response.status());
    return response;
  }

  private ClinicalEncounter findPreviousEncounter(List<ClinicalEncounter> encounters, ClinicalEncounter current) {
    ClinicalEncounter prev = null;
    for (ClinicalEncounter enc : encounters) {
      if (enc.encounterId().equals(current.encounterId())) {
        break;
      }
      prev = enc;
    }
    return prev;
  }

  private boolean isEscalationRequired(
      ClinicalEncounter current, ClinicalEncounter previous, TreatmentResponse response) {

    if (response.status() == TreatmentResponseStatus.WORSENING) {
      return true;
    }
    if (current.urgency() == TriageUrgency.EMERGENCY) {
      return true;
    }
    if (previous != null && getUrgencyRank(current.urgency()) > getUrgencyRank(previous.urgency())) {
      return true;
    }
    return current.decisionSupport() != null
        && current.decisionSupport().veterinarianReviewFlag() != null
        && current.decisionSupport().veterinarianReviewFlag().requiresReview();
  }

  private void handleEscalation(ClinicalCase clinicalCase, ClinicalEncounter current, TreatmentResponse response) {
    log.warn("Escalation safety trigger activated for caseId={} encounterId={}", clinicalCase.caseId(), current.encounterId());

    if (clinicalCase.status() != ClinicalCaseStatus.REFERRED) {
      repository.updateCaseStatus(clinicalCase.caseId(), ClinicalCaseStatus.REFERRED);
    }

    repository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            clinicalCase.caseId(),
            Instant.now(),
            ClinicalTimelineEventType.REFERRAL,
            "Clinical condition worsened or escalation triggered. Referral initiated.",
            current.encounterId(),
            Map.of("urgency", current.urgency().name(), "response", response.status().name())));

    updateOpenFollowUps(clinicalCase.caseId(), FollowUpStatus.ESCALATED);

    if (eventPublisher != null) {
      eventPublisher.publishEvent(
          new ClinicalConditionWorsenedEvent(clinicalCase.caseId(), current.encounterId(), current.urgency(), Instant.now()));
    }
    if (metricsCollector != null) {
      metricsCollector.recordConditionWorsened(current.urgency().name());
    }
  }

  private void handleNormalProgress(UUID caseId, ClinicalEncounter current, TreatmentResponse response) {
    repository.appendTimelineEvent(
        new ClinicalTimelineEvent(
            UUID.randomUUID(),
            caseId,
            Instant.now(),
            ClinicalTimelineEventType.TREATMENT_RESPONSE_RECORDED,
            "Treatment response assessed: " + response.status(),
            current.encounterId(),
            Map.of("status", response.status().name())));

    updateOpenFollowUps(caseId, FollowUpStatus.COMPLETED);
  }

  private void updateOpenFollowUps(UUID caseId, FollowUpStatus targetStatus) {
    List<ClinicalFollowUp> followUps = repository.findFollowUpsByCaseId(caseId);
    for (ClinicalFollowUp f : followUps) {
      if (f.status() == FollowUpStatus.SCHEDULED || f.status() == FollowUpStatus.DUE) {
        repository.updateFollowUpStatus(f.followUpId(), targetStatus);
      }
    }
  }

  private int getUrgencyRank(TriageUrgency urgency) {
    return switch (urgency) {
      case EMERGENCY -> 4;
      case URGENT -> 3;
      case PRIORITY -> 2;
      case ROUTINE -> 1;
    };
  }
}
