package app.vetra.ai.workflow.clinical.clinicalcase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.vetra.ai.event.TreatmentResponseRecordedEvent;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.workflow.clinical.clinicalcase.analysis.ClinicalProgressAnalyzer;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounter;
import app.vetra.ai.workflow.clinical.clinicalcase.encounter.ClinicalEncounterType;
import app.vetra.ai.workflow.clinical.clinicalcase.model.ClinicalCase;
import app.vetra.ai.workflow.clinical.clinicalcase.repository.InMemoryClinicalCaseRepository;
import app.vetra.ai.workflow.clinical.clinicalcase.response.TreatmentResponse;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class FollowUpAssessmentServiceTest {

  private InMemoryClinicalCaseRepository repository;
  private ClinicalProgressAnalyzer progressAnalyzer;
  private ApplicationEventPublisher eventPublisher;
  private AIMetricsCollector metricsCollector;
  private FollowUpAssessmentService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryClinicalCaseRepository();
    progressAnalyzer = new ClinicalProgressAnalyzer();
    eventPublisher = mock(ApplicationEventPublisher.class);
    metricsCollector = mock(AIMetricsCollector.class);
    service = new FollowUpAssessmentService(repository, progressAnalyzer, eventPublisher, metricsCollector);
  }

  @Test
  void testEvaluateEncounterProgress_success() {
    UUID animalId = UUID.randomUUID();
    ClinicalCase c = repository.createCase(new ClinicalCase(UUID.randomUUID(), animalId, "BOVINE", "Holstein", "Mastitis", Instant.now(), Instant.now(), null, null));

    ClinicalEncounter curr =
        new ClinicalEncounter(
            UUID.randomUUID(),
            c.caseId(),
            UUID.randomUUID(),
            Instant.now(),
            ClinicalEncounterType.INITIAL_ASSESSMENT,
            TriageUrgency.URGENT,
            "Mastitis",
            BigDecimal.valueOf(0.90),
            List.of(),
            null,
            null,
            null,
            null);
    repository.saveEncounter(curr);

    TreatmentResponse response = service.evaluateEncounterProgress(c.caseId(), curr);

    assertNotNull(response);
    verify(eventPublisher).publishEvent(any(TreatmentResponseRecordedEvent.class));
  }
}
