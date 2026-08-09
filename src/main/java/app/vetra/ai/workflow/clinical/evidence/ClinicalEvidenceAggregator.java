package app.vetra.ai.workflow.clinical.evidence;

import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.workflow.clinical.model.evidence.AbnormalityStatus;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.ClinicalHistory;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceSource;
import app.vetra.ai.workflow.clinical.model.evidence.EvidenceType;
import app.vetra.ai.workflow.clinical.model.evidence.LaboratoryResult;
import app.vetra.ai.workflow.clinical.model.evidence.SensorObservation;
import app.vetra.ai.workflow.clinical.model.evidence.UnifiedClinicalEvidence;
import app.vetra.ai.workflow.clinical.model.evidence.VitalSign;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Normalizes multi-modal clinical findings, preserves provenance, distinguishes genuine conflicts
 * from temporal measurement trends, and produces a unified clinical evidence collection.
 */
@Component
public class ClinicalEvidenceAggregator {

  private static final Logger log = LoggerFactory.getLogger(ClinicalEvidenceAggregator.class);

  /**
   * Aggregates all available evidence streams into a {@link UnifiedClinicalEvidence} collection.
   *
   * @param symptoms text symptoms list
   * @param diagnosisResponse visual pathology response from DiagnosisStep
   * @param labResults laboratory results
   * @param vitalSigns physiological vital sign readings
   * @param sensorObservations IoT sensor telemetry
   * @param clinicalHistory past medical history
   * @return aggregated {@link UnifiedClinicalEvidence}
   */
  public UnifiedClinicalEvidence aggregateEvidence(
      List<String> symptoms,
      AgentResponse diagnosisResponse,
      List<LaboratoryResult> labResults,
      List<VitalSign> vitalSigns,
      List<SensorObservation> sensorObservations,
      List<ClinicalHistory> clinicalHistory) {

    log.debug("ClinicalEvidenceAggregator starting aggregation across evidence streams");

    List<ClinicalEvidence> items = new ArrayList<>();
    List<String> conflicts = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    processSymptoms(symptoms, items);
    processVisualPathology(diagnosisResponse, items);
    processLabResults(labResults, items, warnings);
    processVitalSigns(vitalSigns, items, warnings);
    processSensorObservations(sensorObservations, items, warnings);
    processClinicalHistory(clinicalHistory, items);

    detectConflicts(vitalSigns, sensorObservations, conflicts);

    log.info(
        "ClinicalEvidenceAggregator aggregated {} items, {} conflicts, {} warnings",
        items.size(),
        conflicts.size(),
        warnings.size());

    return new UnifiedClinicalEvidence(items, conflicts, warnings, Instant.now());
  }

  private void processSymptoms(List<String> symptoms, List<ClinicalEvidence> items) {
    if (symptoms == null || symptoms.isEmpty()) {
      return;
    }
    List<String> cleanSymptoms = symptoms.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
    if (!cleanSymptoms.isEmpty()) {
      items.add(
          new ClinicalEvidence(
              UUID.randomUUID().toString(),
              EvidenceType.SYMPTOM,
              EvidenceSource.VET_OBSERVATION,
              "Reported Symptoms: " + String.join(", ", cleanSymptoms),
              cleanSymptoms,
              BigDecimal.valueOf(1.00),
              AbnormalityStatus.NORMAL,
              Instant.now(),
              Map.of()));
    }
  }

  private void processVisualPathology(AgentResponse diagnosisResponse, List<ClinicalEvidence> items) {
    if (diagnosisResponse == null || diagnosisResponse.rawResponse() == null) {
      return;
    }
    String content = diagnosisResponse.rawResponse().content();
    if (content != null && !content.isBlank()) {
      items.add(
          new ClinicalEvidence(
              UUID.randomUUID().toString(),
              EvidenceType.IMAGE,
              EvidenceSource.AI_VISION,
              "Visual Pathology Finding: " + content.trim(),
              List.of(content.trim()),
              BigDecimal.valueOf(0.90),
              AbnormalityStatus.NORMAL,
              Instant.now(),
              Map.of("agentName", diagnosisResponse.agentName())));
    }
  }

  private void processLabResults(List<LaboratoryResult> labResults, List<ClinicalEvidence> items, List<String> warnings) {
    if (labResults == null) {
      return;
    }
    for (LaboratoryResult lab : labResults) {
      if (lab == null || lab.testName().isBlank()) {
        warnings.add("Skipped unverified/empty laboratory result entry");
        continue;
      }
      String summary = String.format("%s: %s %s (Ref: %s, Status: %s)", lab.testName(), lab.value(), lab.unit(), lab.referenceRange(), lab.status());
      Map<String, String> meta = new HashMap<>();
      meta.put("testName", lab.testName());
      meta.put("referenceRange", lab.referenceRange());
      if (!lab.notes().isBlank()) {
        meta.put("notes", lab.notes());
      }
      items.add(
          new ClinicalEvidence(
              UUID.randomUUID().toString(),
              EvidenceType.LAB_RESULT,
              EvidenceSource.LABORATORY,
              summary,
              List.of(summary),
              BigDecimal.valueOf(1.00),
              lab.status(),
              lab.timestamp(),
              meta));
    }
  }

  private void processVitalSigns(List<VitalSign> vitalSigns, List<ClinicalEvidence> items, List<String> warnings) {
    if (vitalSigns == null) {
      return;
    }
    for (VitalSign vital : vitalSigns) {
      if (vital == null || vital.measurementType().isBlank()) {
        warnings.add("Skipped unverified vital sign entry");
        continue;
      }
      String summary = String.format("%s: %.2f %s (Status: %s)", vital.measurementType(), vital.value(), vital.unit(), vital.status());
      items.add(
          new ClinicalEvidence(
              UUID.randomUUID().toString(),
              EvidenceType.VITAL_SIGN,
              EvidenceSource.VET_OBSERVATION,
              summary,
              List.of(summary),
              BigDecimal.valueOf(1.00),
              vital.status(),
              vital.timestamp(),
              Map.of("type", vital.measurementType(), "val", String.valueOf(vital.value()))));
    }
  }

  private void processSensorObservations(List<SensorObservation> sensorObservations, List<ClinicalEvidence> items, List<String> warnings) {
    if (sensorObservations == null) {
      return;
    }
    for (SensorObservation sensor : sensorObservations) {
      if (sensor == null || sensor.sensorType().isBlank()) {
        warnings.add("Skipped invalid sensor observation entry");
        continue;
      }
      String summary = String.format("Sensor %s (%s): %.2f %s [%s]", sensor.sensorType(), sensor.sensorId(), sensor.reading(), sensor.unit(), sensor.status());
      items.add(
          new ClinicalEvidence(
              UUID.randomUUID().toString(),
              EvidenceType.SENSOR_OBSERVATION,
              EvidenceSource.IOT_SENSOR,
              summary,
              List.of(summary),
              BigDecimal.valueOf(0.95),
              AbnormalityStatus.NORMAL,
              sensor.timestamp(),
              Map.of("sensorType", sensor.sensorType())));
    }
  }

  private void processClinicalHistory(List<ClinicalHistory> clinicalHistory, List<ClinicalEvidence> items) {
    if (clinicalHistory == null) {
      return;
    }
    for (ClinicalHistory hist : clinicalHistory) {
      if (hist == null || hist.conditionOrEvent().isBlank()) {
        continue;
      }
      String summary = String.format("History: %s (Status: %s, Tx: %s)", hist.conditionOrEvent(), hist.outcomeStatus(), hist.treatmentGiven());
      items.add(
          new ClinicalEvidence(
              UUID.randomUUID().toString(),
              EvidenceType.CLINICAL_HISTORY,
              EvidenceSource.CLINICAL_RECORD,
              summary,
              List.of(summary),
              BigDecimal.valueOf(1.00),
              AbnormalityStatus.NORMAL,
              hist.diagnosedAt(),
              Map.of("outcome", hist.outcomeStatus())));
    }
  }

  private void detectConflicts(
      List<VitalSign> vitalSigns,
      List<SensorObservation> sensorObservations,
      List<String> conflicts) {

    if (vitalSigns == null || sensorObservations == null) {
      return;
    }

    for (VitalSign vital : vitalSigns) {
      if ("TEMPERATURE".equalsIgnoreCase(vital.measurementType())) {
        for (SensorObservation sensor : sensorObservations) {
          if ("BODY_TEMP".equalsIgnoreCase(sensor.sensorType()) || "TEMPERATURE".equalsIgnoreCase(sensor.sensorType())) {
            long minutesApart = Math.abs(Duration.between(vital.timestamp(), sensor.timestamp()).toMinutes());
            if (minutesApart <= 15) {
              boolean statusConflict =
                  (vital.status() == AbnormalityStatus.HIGH || vital.status() == AbnormalityStatus.CRITICAL)
                      && "OK".equalsIgnoreCase(sensor.status());
              double diff = Math.abs(vital.value() - sensor.reading());
              if (statusConflict && diff > 1.5) {
                conflicts.add(
                    String.format(
                        "Concurrent Temperature Conflict: Vital Sign recorded %.1f %s (%s) while Sensor %s recorded %.1f %s (%s) within %d mins",
                        vital.value(), vital.unit(), vital.status(), sensor.sensorId(), sensor.reading(), sensor.unit(), sensor.status(), minutesApart));
              }
            }
          }
        }
      }
    }
  }
}
