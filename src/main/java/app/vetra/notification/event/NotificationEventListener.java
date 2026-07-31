package app.vetra.notification.event;

import app.vetra.ai.event.AIInferenceCompletedEvent;
import app.vetra.ai.event.AIScanVerifiedEvent;
import app.vetra.appointment.event.AppointmentBookedEvent;
import app.vetra.disease.event.DiseaseReportCreatedEvent;
import app.vetra.disease.event.OutbreakResolvedAutomaticallyEvent;
import app.vetra.disease.event.PotentialOutbreakDetectedEvent;
import app.vetra.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event-driven notification listener consuming domain events across bounded contexts.
 */
@Component
public class NotificationEventListener {

  private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

  private final NotificationService notificationService;

  /** Constructor injection. */
  public NotificationEventListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** Consumes AppointmentBookedEvent. */
  @Async
  @EventListener
  public void handleAppointmentBooked(AppointmentBookedEvent event) {
    log.info("NotificationListener: handleAppointmentBooked appointmentId={}", event.appointmentId());
  }

  /** Consumes AIInferenceCompletedEvent. */
  @Async
  @EventListener
  public void handleAIInferenceCompleted(AIInferenceCompletedEvent event) {
    log.info("NotificationListener: handleAIInferenceCompleted scanId={}", event.scanId());
  }

  /** Consumes AIScanVerifiedEvent. */
  @Async
  @EventListener
  public void handleAIScanVerified(AIScanVerifiedEvent event) {
    log.info("NotificationListener: handleAIScanVerified scanId={}", event.scanId());
  }

  /** Consumes DiseaseReportCreatedEvent. */
  @Async
  @EventListener
  public void handleDiseaseReportCreated(DiseaseReportCreatedEvent event) {
    log.info("NotificationListener: handleDiseaseReportCreated reportId={} disease='{}'", event.reportId(), event.diseaseName());
  }

  /** Consumes PotentialOutbreakDetectedEvent. */
  @Async
  @EventListener
  public void handleOutbreakDetected(PotentialOutbreakDetectedEvent event) {
    log.warn("NotificationListener: handleOutbreakDetected disease='{}' clusterSize={}", event.diseaseName(), event.reportCount());
  }

  /** Consumes OutbreakResolvedAutomaticallyEvent. */
  @Async
  @EventListener
  public void handleOutbreakResolved(OutbreakResolvedAutomaticallyEvent event) {
    log.info("NotificationListener: handleOutbreakResolved outbreakId={} disease='{}'", event.outbreakId(), event.diseaseName());
  }
}
