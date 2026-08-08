package app.vetra.ai.workflow.clinical.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.workflow.clinical.model.DiseaseCandidate;
import app.vetra.ai.workflow.clinical.model.TriageAssessment;
import app.vetra.ai.workflow.clinical.model.TriageRequest;
import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalTriageRulesTest {

  private ClinicalTriageRules rules;

  @BeforeEach
  void setUp() {
    rules = new ClinicalTriageRules();
  }

  @Test
  void testCriticalRespiratoryDistress_triggersEmergency() {
    TriageRequest request =
        new TriageRequest(
            "CATTLE",
            "Holstein",
            List.of("High fever", "Severe respiratory distress"),
            List.of(),
            List.of(),
            "",
            null,
            null);

    Optional<TriageAssessment> result = rules.evaluateRules(request);

    assertTrue(result.isPresent());
    assertEquals(TriageUrgency.EMERGENCY, result.get().urgency());
    assertTrue(result.get().requiresImmediateVeterinaryReview());
    assertFalse(result.get().warningSigns().isEmpty());
  }

  @Test
  void testNegatedRespiratoryDistress_doesNotTriggerEmergency() {
    TriageRequest request =
        new TriageRequest(
            "CATTLE",
            "Holstein",
            List.of("Mild lethargy", "No respiratory distress", "denies collapse", "without bleeding"),
            List.of(),
            List.of(),
            "",
            null,
            null);

    Optional<TriageAssessment> result = rules.evaluateRules(request);

    assertFalse(result.isPresent(), "Negated assertions like 'no respiratory distress' must NOT trigger emergency rules");
  }

  @Test
  void testSevereDiseaseNameAlone_doesNotTriggerEmergencyWithoutClinicalIndicators() {
    DiseaseCandidate severeDisease =
        new DiseaseCandidate(
            "Anthrax",
            BigDecimal.valueOf(0.85),
            "Literature mention",
            List.of(),
            true);

    TriageRequest request =
        new TriageRequest(
            "BOVINE",
            "Angus",
            List.of("Mild appetite loss"),
            List.of("Minor skin lesion"),
            List.of(severeDisease),
            "",
            null,
            null);

    Optional<TriageAssessment> result = rules.evaluateRules(request);

    assertFalse(
        result.isPresent(),
        "Disease severity alone must NOT trigger deterministic emergency without current critical clinical indicators");
  }

  @Test
  void testCollapseAndInabilityToStand_triggersEmergency() {
    TriageRequest request =
        new TriageRequest(
            "BOVINE",
            "Jersey",
            List.of("Inability to stand", "Collapsed in pasture"),
            List.of(),
            List.of(),
            "",
            null,
            null);

    Optional<TriageAssessment> result = rules.evaluateRules(request);

    assertTrue(result.isPresent());
    assertEquals(TriageUrgency.EMERGENCY, result.get().urgency());
  }
}
