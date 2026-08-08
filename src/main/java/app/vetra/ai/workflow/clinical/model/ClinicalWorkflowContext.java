package app.vetra.ai.workflow.clinical.model;

import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.rag.model.RetrievedContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared, stateful execution context carrying intermediate outputs, timings, and metadata
 * across all Clinical Diagnosis Workflow steps.
 */
public class ClinicalWorkflowContext {

  private final ClinicalWorkflowRequest request;
  private final long startTimeNanos;
  private final Map<String, Long> stepTimings;
  private final Map<String, WorkflowStatus> stepStatuses;
  private final Map<String, Object> metadata;
  private final List<String> errors;

  private WorkflowStatus status;
  private AgentResponse diagnosisResponse;
  private RetrievedContext retrievedContext;
  private List<DiseaseCandidate> rankedDiseases;
  private TreatmentPlan treatmentPlan;
  private ClinicalDiagnosisReport report;
  private long totalDurationMs;

  /**
   * Initializes a new workflow context with an initial request.
   *
   * @param request clinical workflow request
   */
  public ClinicalWorkflowContext(ClinicalWorkflowRequest request) {
    this.request = request;
    this.startTimeNanos = System.nanoTime();
    this.status = WorkflowStatus.RUNNING;
    this.stepTimings = new ConcurrentHashMap<>();
    this.stepStatuses = new ConcurrentHashMap<>();
    this.metadata = new ConcurrentHashMap<>();
    this.errors = Collections.synchronizedList(new ArrayList<>());
    this.rankedDiseases = new ArrayList<>();
  }

  public ClinicalWorkflowRequest getRequest() {
    return request;
  }

  public long getStartTimeNanos() {
    return startTimeNanos;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }

  public AgentResponse getDiagnosisResponse() {
    return diagnosisResponse;
  }

  public void setDiagnosisResponse(AgentResponse diagnosisResponse) {
    this.diagnosisResponse = diagnosisResponse;
  }

  public RetrievedContext getRetrievedContext() {
    return retrievedContext;
  }

  public void setRetrievedContext(RetrievedContext retrievedContext) {
    this.retrievedContext = retrievedContext;
  }

  public List<DiseaseCandidate> getRankedDiseases() {
    return Collections.unmodifiableList(rankedDiseases);
  }

  public void setRankedDiseases(List<DiseaseCandidate> rankedDiseases) {
    this.rankedDiseases = rankedDiseases != null ? new ArrayList<>(rankedDiseases) : new ArrayList<>();
  }

  public TreatmentPlan getTreatmentPlan() {
    return treatmentPlan;
  }

  public void setTreatmentPlan(TreatmentPlan treatmentPlan) {
    this.treatmentPlan = treatmentPlan;
  }

  public ClinicalDiagnosisReport getReport() {
    return report;
  }

  public void setReport(ClinicalDiagnosisReport report) {
    this.report = report;
  }

  public long getTotalDurationMs() {
    return totalDurationMs;
  }

  public void setTotalDurationMs(long totalDurationMs) {
    this.totalDurationMs = totalDurationMs;
  }

  public Map<String, Long> getStepTimings() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(stepTimings));
  }

  public void recordStepTiming(String stepName, long durationMs) {
    if (stepName != null) {
      stepTimings.put(stepName, durationMs);
    }
  }

  public Map<String, WorkflowStatus> getStepStatuses() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(stepStatuses));
  }

  public void recordStepStatus(String stepName, WorkflowStatus stepStatus) {
    if (stepName != null && stepStatus != null) {
      stepStatuses.put(stepName, stepStatus);
    }
  }

  public Map<String, Object> getMetadata() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
  }

  public void putMetadata(String key, Object value) {
    if (key != null && value != null) {
      metadata.put(key, value);
    }
  }

  public List<String> getErrors() {
    return Collections.unmodifiableList(new ArrayList<>(errors));
  }

  public void addError(String error) {
    if (error != null && !error.isBlank()) {
      errors.add(error.trim());
    }
  }
}
