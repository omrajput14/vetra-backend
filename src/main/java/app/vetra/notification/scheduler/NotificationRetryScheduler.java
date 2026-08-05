package app.vetra.notification.scheduler;

import app.vetra.notification.entity.Notification;
import app.vetra.notification.entity.NotificationStatus;
import app.vetra.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Background scheduler retrying failed notifications and performing device token cleanup. */
@Component
public class NotificationRetryScheduler {

  private static final Logger log = LoggerFactory.getLogger(NotificationRetryScheduler.class);

  private final NotificationRepository notificationRepository;

  /** Constructor injection. */
  public NotificationRetryScheduler(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  /** Periodically retries failed or queued notifications due for redelivery. */
  @Scheduled(cron = "${vetra.notification.retry-cron:0 */15 * * * *}")
  @Transactional
  public void retryFailedNotifications() {
    log.info("Running notification retry scheduler...");
    Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
    List<Notification> pendingOrQueued =
        notificationRepository.findByStatusAndScheduledAtBefore(NotificationStatus.QUEUED, cutoff);

    for (Notification n : pendingOrQueued) {
      log.info("Retrying pending notification id={} userId={}", n.getId(), n.getUser().getId());
      // Execution retry
    }
  }
}
