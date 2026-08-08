package app.vetra.ai.workflow.clinical.model;

import java.util.List;

/**
 * Structured treatment plan output synthesized from the TreatmentAgent response.
 *
 * @param primaryTreatment primary recommended treatment strategy
 * @param medications list of recommended medications with dosage guidelines
 * @param precautions immediate clinical and biosecurity precautions
 * @param monitoringAdvice observation and monitoring advice for the farmer/veterinarian
 * @param followUpDays recommended follow-up inspection interval in days
 */
public record TreatmentPlan(
    String primaryTreatment,
    List<String> medications,
    List<String> precautions,
    List<String> monitoringAdvice,
    int followUpDays) {

  /** Canonical constructor with non-null defaults. */
  public TreatmentPlan {
    primaryTreatment = primaryTreatment != null ? primaryTreatment.trim() : "Standard supportive care.";
    medications = medications != null ? List.copyOf(medications) : List.of();
    precautions = precautions != null ? List.copyOf(precautions) : List.of();
    monitoringAdvice = monitoringAdvice != null ? List.copyOf(monitoringAdvice) : List.of();
    if (followUpDays <= 0) {
      followUpDays = 3;
    }
  }

  /** Default fallback treatment plan when generation produces empty output. */
  public static TreatmentPlan defaultPlan(String condition) {
    return new TreatmentPlan(
        "Supportive clinical care and isolation for " + condition,
        List.of("Consult licensed veterinarian before administration"),
        List.of("Isolate affected animal", "Maintain strict hygiene and biosecurity"),
        List.of("Monitor body temperature and feed intake twice daily"),
        3);
  }
}
