package app.vetra.notification.event;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.event.AIInferenceCompletedEvent;
import app.vetra.ai.event.AIScanVerifiedEvent;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.appointment.event.AppointmentBookedEvent;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.disease.event.DiseaseReportCreatedEvent;
import app.vetra.disease.event.OutbreakResolvedAutomaticallyEvent;
import app.vetra.disease.event.PotentialOutbreakDetectedEvent;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.service.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Event-driven notification listener consuming domain events across bounded contexts. */
@Component
public class NotificationEventListener {

  private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

  private final NotificationService notificationService;
  private final AIScanRepository aiScanRepository;
  private final VetProfileRepository vetProfileRepository;

  /** Constructor injection. */
  public NotificationEventListener(
      NotificationService notificationService,
      AIScanRepository aiScanRepository,
      VetProfileRepository vetProfileRepository) {
    this.notificationService = notificationService;
    this.aiScanRepository = aiScanRepository;
    this.vetProfileRepository = vetProfileRepository;
  }

  /** Consumes AppointmentBookedEvent. */
  @Async
  @EventListener
  public void handleAppointmentBooked(AppointmentBookedEvent event) {
    log.info(
        "NotificationListener: handleAppointmentBooked appointmentId={}", event.appointmentId());
  }

  /** Consumes AIInferenceCompletedEvent. */
  @Async
  @EventListener
  public void handleAIInferenceCompleted(AIInferenceCompletedEvent event) {
    log.info("NotificationListener: handleAIInferenceCompleted scanId={}", event.scanId());
    try {
      Optional<AIScan> scanOpt = aiScanRepository.findById(event.scanId());
      if (scanOpt.isPresent()) {
        AIScan scan = scanOpt.get();
        UUID userId = scan.getUploadedBy() != null ? scan.getUploadedBy().getId() : null;
        if (userId != null) {
          String payload = "{\"scanId\":\"" + event.scanId() + "\",\"route\":\"/ai-history\"}";
          notificationService.sendNotification(
              userId,
              "AI Diagnostic Scan Ready",
              "Your animal diagnostic scan inference is complete. Tap to review differential diagnosis.",
              payload,
              NotificationChannel.PUSH,
              NotificationPriority.NORMAL);
        }
      }
    } catch (Throwable t) {
      log.warn("Failed to dispatch AI inference completed notification: {}", t.getMessage());
    }
  }

  /** Consumes AIScanVerifiedEvent. */
  @Async
  @EventListener
  public void handleAIScanVerified(AIScanVerifiedEvent event) {
    log.info("NotificationListener: handleAIScanVerified scanId={}", event.scanId());
    try {
      Optional<AIScan> scanOpt = aiScanRepository.findById(event.scanId());
      if (scanOpt.isPresent()) {
        AIScan scan = scanOpt.get();
        UUID userId = scan.getUploadedBy() != null ? scan.getUploadedBy().getId() : null;
        if (userId != null) {
          String status = event.accepted() ? "approved" : "reviewed";
          String payload = "{\"scanId\":\"" + event.scanId() + "\",\"route\":\"/ai-history\"}";
          notificationService.sendNotification(
              userId,
              "AI Scan Reviewed by Veterinarian",
              "A licensed veterinarian has " + status + " your animal diagnostic scan result.",
              payload,
              NotificationChannel.PUSH,
              NotificationPriority.NORMAL);
        }
      }
    } catch (Throwable t) {
      log.warn("Failed to dispatch AI scan verified notification: {}", t.getMessage());
    }
  }

  /** Consumes DiseaseReportCreatedEvent. */
  @Async
  @EventListener
  public void handleDiseaseReportCreated(DiseaseReportCreatedEvent event) {
    log.info(
        "NotificationListener: handleDiseaseReportCreated reportId={} disease='{}'",
        event.reportId(),
        event.diseaseName());
  }

  /** Consumes PotentialOutbreakDetectedEvent. */
  @Async
  @EventListener
  public void handleOutbreakDetected(PotentialOutbreakDetectedEvent event) {
    log.warn(
        "NotificationListener: handleOutbreakDetected disease='{}' clusterSize={}",
        event.diseaseName(),
        event.reportCount());
    try {
      List<VetProfile> activeVets =
          vetProfileRepository.findAllActiveNonRejected(VerificationStatus.REJECTED);
      String payload = "{\"disease\":\"" + event.diseaseName() + "\",\"route\":\"/outbreaks\"}";
      for (VetProfile vet : activeVets) {
        if (vet.getUser() != null) {
          notificationService.sendNotification(
              vet.getUser().getId(),
              "Outbreak Alert: " + event.diseaseName(),
              "Potential outbreak cluster of "
                  + event.diseaseName()
                  + " ("
                  + event.reportCount()
                  + " cases) detected in your operational sector.",
              payload,
              NotificationChannel.PUSH,
              NotificationPriority.HIGH);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to dispatch outbreak alert notifications: {}", e.getMessage());
    }
  }

  /** Consumes OutbreakResolvedAutomaticallyEvent. */
  @Async
  @EventListener
  public void handleOutbreakResolved(OutbreakResolvedAutomaticallyEvent event) {
    log.info(
        "NotificationListener: handleOutbreakResolved outbreakId={} disease='{}'",
        event.outbreakId(),
        event.diseaseName());
  }
}
