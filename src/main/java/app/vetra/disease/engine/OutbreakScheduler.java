package app.vetra.disease.engine;

import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.entity.OutbreakTrend;
import app.vetra.disease.event.OutbreakMonitoringEvent;
import app.vetra.disease.event.OutbreakResolvedAutomaticallyEvent;
import app.vetra.disease.event.OutbreakTrendChangedEvent;
import app.vetra.disease.repository.OutbreakRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autonomous background scheduler executing periodic epidemiological re-evaluation
 * and automated outbreak cluster lifecycle transitions.
 */
@Component
public class OutbreakScheduler {

  private static final Logger log = LoggerFactory.getLogger(OutbreakScheduler.class);

  private final OutbreakRepository outbreakRepository;
  private final OutbreakDetectionEngine outbreakEngine;
  private final OutbreakTrendAnalyzer trendAnalyzer;
  private final ApplicationEventPublisher eventPublisher;

  /** Constructor injection. */
  public OutbreakScheduler(
      OutbreakRepository outbreakRepository,
      OutbreakDetectionEngine outbreakEngine,
      OutbreakTrendAnalyzer trendAnalyzer,
      ApplicationEventPublisher eventPublisher) {
    this.outbreakRepository = outbreakRepository;
    this.outbreakEngine = outbreakEngine;
    this.trendAnalyzer = trendAnalyzer;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Periodic hourly cron task evaluating active and monitoring outbreak clusters.
   */
  @Scheduled(cron = "${vetra.disease.outbreak.cron:0 0 * * * *}")
  @Transactional
  public void runScheduledEvaluation() {
    log.info("Running autonomous disease outbreak evaluation scheduler...");

    List<Outbreak> activeOrMonitoring = outbreakRepository.findAll()
        .stream()
        .filter(o -> o.getStatus() != OutbreakStatus.RESOLVED)
        .toList();

    Instant now = Instant.now();

    for (Outbreak outbreak : activeOrMonitoring) {
      evaluateSingleOutbreak(outbreak, now);
    }
  }

  /**
   * Evaluates a single outbreak cluster for lifecycle transitions and risk/trend updates.
   */
  @Transactional
  public void evaluateSingleOutbreak(Outbreak outbreak, Instant now) {
    long hoursSinceLastCase = outbreak.getLastCaseReportedAt() != null
        ? ChronoUnit.HOURS.between(outbreak.getLastCaseReportedAt(), now)
        : ChronoUnit.HOURS.between(outbreak.getCreatedAt(), now);

    int windowHours = outbreak.getEvaluationWindowHours();
    OutbreakTrend oldTrend = outbreak.getTrend();
    OutbreakTrend newTrend = trendAnalyzer.calculateTrend(outbreak);

    if (oldTrend != newTrend) {
      outbreak.setTrend(newTrend);
      eventPublisher.publishEvent(new OutbreakTrendChangedEvent(outbreak.getId(), outbreak.getDiseaseName(), oldTrend, newTrend));
    }

    // Lifecycle Automation Rules
    if (hoursSinceLastCase >= 2L * windowHours && outbreak.getStatus() == OutbreakStatus.MONITORING) {
      // Transition MONITORING -> RESOLVED
      outbreak.setStatus(OutbreakStatus.RESOLVED);
      outbreak.setResolvedAt(now);
      outbreak.setResolutionReason("AUTOMATIC_INACTIVITY_TIMEOUT");
      outbreak.setRiskScore(OutbreakRiskScore.LOW);
      outbreakRepository.save(outbreak);

      log.info("AUTONOMOUS OUTBREAK RESOLUTION: id={} disease='{}' reason='AUTOMATIC_INACTIVITY_TIMEOUT'",
          outbreak.getId(), outbreak.getDiseaseName());

      eventPublisher.publishEvent(new OutbreakResolvedAutomaticallyEvent(
          outbreak.getId(), outbreak.getDiseaseName(), outbreak.getResolutionReason(), now));

    } else if (hoursSinceLastCase >= windowHours && outbreak.getStatus() == OutbreakStatus.ACTIVE) {
      // Transition ACTIVE -> MONITORING
      outbreak.setStatus(OutbreakStatus.MONITORING);
      outbreakRepository.save(outbreak);

      log.info("AUTONOMOUS OUTBREAK MONITORING TRANSITION: id={} disease='{}'", outbreak.getId(), outbreak.getDiseaseName());
      eventPublisher.publishEvent(new OutbreakMonitoringEvent(outbreak.getId(), outbreak.getDiseaseName(), now));
    }

    outbreak.setLastEvaluatedAt(now);
    outbreakRepository.save(outbreak);
  }
}
