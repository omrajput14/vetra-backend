package app.vetra.developer.service;

import app.vetra.ai.repository.AIAdvisorSessionRepository;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/** Holder component encapsulating surveillance and intelligence repositories. */
@Component
public class SurveillanceDataHolder {

  private final DiseaseReportRepository diseaseReportRepository;
  private final OutbreakRepository outbreakRepository;
  private final AIScanRepository aiScanRepository;
  private final AIAdvisorSessionRepository aiAdvisorSessionRepository;
  private final NotificationRepository notificationRepository;

  public SurveillanceDataHolder(
      DiseaseReportRepository diseaseReportRepository,
      OutbreakRepository outbreakRepository,
      AIScanRepository aiScanRepository,
      AIAdvisorSessionRepository aiAdvisorSessionRepository,
      NotificationRepository notificationRepository) {
    this.diseaseReportRepository = diseaseReportRepository;
    this.outbreakRepository = outbreakRepository;
    this.aiScanRepository = aiScanRepository;
    this.aiAdvisorSessionRepository = aiAdvisorSessionRepository;
    this.notificationRepository = notificationRepository;
  }

  public DiseaseReportRepository reportRepo() {
    return diseaseReportRepository;
  }

  public OutbreakRepository outbreakRepo() {
    return outbreakRepository;
  }

  public AIScanRepository scanRepo() {
    return aiScanRepository;
  }

  public AIAdvisorSessionRepository advisorRepo() {
    return aiAdvisorSessionRepository;
  }

  public NotificationRepository notificationRepo() {
    return notificationRepository;
  }
}
