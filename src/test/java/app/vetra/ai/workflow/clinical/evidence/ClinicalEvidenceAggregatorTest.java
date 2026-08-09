package app.vetra.ai.workflow.clinical.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.model.AIResponse;
import app.vetra.ai.workflow.clinical.model.evidence.AbnormalityStatus;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalHistory;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceSource;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceType;
import app.vetra.ai.workflow.clinical.model.evidence.LaboratoryResult;
import app.vetra.ai.workflow.clinical.model.evidence.SensorObservation;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.VitalSign;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClinicalEvidenceAggregatorTest {

  private ClinicalEvidenceAggregator aggregator;

  @BeforeEach
  void setUp() {
    aggregator = new ClinicalEvidenceAggregator();
  }

  @Test
  void testAggregateMultipleEvidenceTypes() {
    List<String> symptoms = List.of("High fever", "Loss of appetite");
    AgentResponse diagnosisResponse = mock(AgentResponse.class);
    AIResponse rawResponse = mock(AIResponse.class);
    when(rawResponse.content()).thenReturn("{\"condition\":\"Bovine Mastitis\",\"confidence\":0.88}");
    when(diagnosisResponse.rawResponse()).thenReturn(rawResponse);
    when(diagnosisResponse.agentName()).thenReturn("DiagnosisAgent");

    LaboratoryResult lab =
        new LaboratoryResult(
            "Somatic Cell Count",
            "750,000",
            "cells/mL",
            "< 200,000",
            AbnormalityStatus.HIGH,
            Instant.now(),
            "Elevated SCC");

    VitalSign vital =
        new VitalSign(
            "TEMPERATURE",
            40.2,
            "Celsius",
            AbnormalityStatus.HIGH,
            Instant.now());

    SensorObservation sensor =
        new SensorObservation(
            "RUMINATION-101",
            "RUMINATION",
            120.0,
            "minutes/day",
            Instant.now(),
            "DECREASED");

    ClinicalHistory history =
        new ClinicalHistory(
            "Mastitis Episode",
            Instant.now().minus(30, ChronoUnit.DAYS),
            "Intramammary Antibiotics",
            "RESOLVED",
            "Prior occurrence");

    UnifiedClinicalEvidence unified =
        aggregator.aggregateEvidence(
            symptoms,
            diagnosisResponse,
            List.of(lab),
            List.of(vital),
            List.of(sensor),
            List.of(history));

    assertNotNull(unified);
    assertEquals(6, unified.items().size());
    assertFalse(unified.findByType(EvidenceType.SYMPTOM).isEmpty());
    assertFalse(unified.findByType(EvidenceType.IMAGE).isEmpty());
    assertFalse(unified.findByType(EvidenceType.LAB_RESULT).isEmpty());
    assertFalse(unified.findByType(EvidenceType.VITAL_SIGN).isEmpty());
    assertFalse(unified.findByType(EvidenceType.SENSOR_OBSERVATION).isEmpty());
    assertFalse(unified.findByType(EvidenceType.CLINICAL_HISTORY).isEmpty());

    // Verify Provenance
    assertEquals(EvidenceSource.AI_VISION, unified.findByType(EvidenceType.IMAGE).get(0).source());
    assertEquals(EvidenceSource.LABORATORY, unified.findByType(EvidenceType.LAB_RESULT).get(0).source());
    assertEquals(EvidenceSource.IOT_SENSOR, unified.findByType(EvidenceType.SENSOR_OBSERVATION).get(0).source());
  }

  @Test
  void testDetectGenuineConflictVsDiscrepancy() {
    Instant now = Instant.now();

    // Concurrent temperature measurement conflict (< 15 mins apart)
    VitalSign vitalHigh = new VitalSign("TEMPERATURE", 41.0, "C", AbnormalityStatus.CRITICAL, now);
    SensorObservation sensorOk = new SensorObservation("BOLUS-01", "BODY_TEMP", 38.5, "C", now.plusSeconds(120), "OK");

    UnifiedClinicalEvidence unifiedConflict =
        aggregator.aggregateEvidence(
            List.of("Lethargy"),
            null,
            List.of(),
            List.of(vitalHigh),
            List.of(sensorOk),
            List.of());

    assertFalse(unifiedConflict.conflicts().isEmpty());
    assertTrue(unifiedConflict.conflicts().get(0).contains("Concurrent Temperature Conflict"));

    // Temporal discrepancy (> 30 mins apart, e.g. fever earlier vs normal temp now)
    SensorObservation sensorOldFever = new SensorObservation("BOLUS-01", "BODY_TEMP", 41.0, "C", now.minus(2, ChronoUnit.HOURS), "OK");
    SensorObservation sensorNowNormal = new SensorObservation("BOLUS-01", "BODY_TEMP", 38.5, "C", now, "OK");

    UnifiedClinicalEvidence unifiedTemporal =
        aggregator.aggregateEvidence(
            List.of(),
            null,
            List.of(),
            List.of(),
            List.of(sensorOldFever, sensorNowNormal),
            List.of());

    assertTrue(unifiedTemporal.conflicts().isEmpty());
  }

  @Test
  void testMissingAndNullDataHandling() {
    UnifiedClinicalEvidence unified = aggregator.aggregateEvidence(null, null, null, null, null, null);
    assertNotNull(unified);
    assertTrue(unified.items().isEmpty());
    assertTrue(unified.conflicts().isEmpty());
    assertEquals("No multi-modal clinical evidence available.", unified.toClinicalSummaryText());
  }
}
