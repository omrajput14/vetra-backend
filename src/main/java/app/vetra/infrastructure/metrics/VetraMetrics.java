package app.vetra.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralized Micrometer business metrics component for the Vetra Platform.
 *
 * <p>This is the single source of truth for all custom business counters and timers.
 * Only low-cardinality labels are used — no UUIDs, emails, user IDs, or entity IDs.
 *
 * <p>Metric naming convention: {@code vetra.<domain>.<event>}
 */
@Component
public class VetraMetrics {

  // ─── Auth ───────────────────────────────────────────────────────────────
  private final Counter farmerRegistrations;
  private final Counter vetRegistrations;
  private final Counter farmerLoginSuccess;
  private final Counter farmerLoginFailure;
  private final Counter vetLoginSuccess;
  private final Counter vetLoginFailure;

  // ─── Animal ─────────────────────────────────────────────────────────────
  private final Counter animalRegistrations;

  // ─── Appointments ───────────────────────────────────────────────────────
  private final Counter appointmentsCreated;
  private final Counter appointmentsConfirmed;
  private final Counter appointmentsCompleted;
  private final Counter appointmentsCancelled;
  private final Counter appointmentsRejected;

  // ─── AI Diagnostics ─────────────────────────────────────────────────────
  private final Counter aiDiagnosisRequests;

  // ─── Notifications ──────────────────────────────────────────────────────
  private final Counter notificationsSentSuccess;
  private final Counter notificationsSentFailure;
  private final Counter notificationsQueued;

  // ─── Timers ─────────────────────────────────────────────────────────────
  private final Timer appointmentCreateTimer;
  private final Timer animalCreateTimer;

  /** Registers all metric instruments with Micrometer at startup. */
  public VetraMetrics(MeterRegistry registry) {
    farmerRegistrations = authRegistrationCounter(registry, "FARMER");
    vetRegistrations = authRegistrationCounter(registry, "VETERINARIAN");
    farmerLoginSuccess = authLoginCounter(registry, "FARMER", "success");
    farmerLoginFailure = authLoginCounter(registry, "FARMER", "failure");
    vetLoginSuccess = authLoginCounter(registry, "VETERINARIAN", "success");
    vetLoginFailure = authLoginCounter(registry, "VETERINARIAN", "failure");
    animalRegistrations = buildCounter(registry, "vetra.animal.registrations",
        "Total livestock animals registered");
    appointmentsCreated = buildCounter(registry, "vetra.appointments.created",
        "Total appointment bookings created");
    appointmentsConfirmed = appointmentStatusCounter(registry, "CONFIRMED");
    appointmentsCompleted = appointmentStatusCounter(registry, "COMPLETED");
    appointmentsCancelled = appointmentStatusCounter(registry, "CANCELLED");
    appointmentsRejected = appointmentStatusCounter(registry, "REJECTED");
    aiDiagnosisRequests = buildCounter(registry, "vetra.ai.diagnosis.requests",
        "Total AI diagnostic scan requests submitted");
    notificationsSentSuccess = notificationCounter(registry, "success");
    notificationsSentFailure = notificationCounter(registry, "failure");
    notificationsQueued = notificationCounter(registry, "queued");
    appointmentCreateTimer = buildTimer(registry, "vetra.appointments.create.duration",
        "Appointment creation service layer duration");
    animalCreateTimer = buildTimer(registry, "vetra.animal.create.duration",
        "Animal registration service layer duration");
  }

  // ─── Factory helpers ─────────────────────────────────────────────────────

  private static Counter authRegistrationCounter(MeterRegistry r, String role) {
    return Counter.builder("vetra.user.registrations")
        .description("Total user account registrations by role")
        .tag("role", role)
        .register(r);
  }

  private static Counter authLoginCounter(MeterRegistry r, String role, String result) {
    return Counter.builder("vetra.auth.login")
        .description("Total login attempts by role and result")
        .tag("role", role)
        .tag("result", result)
        .register(r);
  }

  private static Counter appointmentStatusCounter(MeterRegistry r, String status) {
    return Counter.builder("vetra.appointments.status")
        .description("Total appointment status transitions")
        .tag("status", status)
        .register(r);
  }

  private static Counter notificationCounter(MeterRegistry r, String result) {
    return Counter.builder("vetra.notifications.dispatched")
        .description("Total notifications dispatched by result")
        .tag("result", result)
        .register(r);
  }

  private static Counter buildCounter(MeterRegistry r, String name, String description) {
    return Counter.builder(name).description(description).register(r);
  }

  private static Timer buildTimer(MeterRegistry r, String name, String description) {
    return Timer.builder(name)
        .description(description)
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(r);
  }

  // ─── Auth ─────────────────────────────────────────────────────────────

  /** Increments farmer registration counter. */
  public void recordFarmerRegistration() {
    farmerRegistrations.increment();
  }

  /** Increments veterinarian registration counter. */
  public void recordVetRegistration() {
    vetRegistrations.increment();
  }

  /** Increments farmer login success counter. */
  public void recordFarmerLoginSuccess() {
    farmerLoginSuccess.increment();
  }

  /** Increments farmer login failure counter. */
  public void recordFarmerLoginFailure() {
    farmerLoginFailure.increment();
  }

  /** Increments veterinarian login success counter. */
  public void recordVetLoginSuccess() {
    vetLoginSuccess.increment();
  }

  /** Increments veterinarian login failure counter. */
  public void recordVetLoginFailure() {
    vetLoginFailure.increment();
  }

  // ─── Animal ──────────────────────────────────────────────────────────

  /** Increments the animal registration counter by one. */
  public void recordAnimalRegistration() {
    animalRegistrations.increment();
  }

  /** Returns the animal create Timer for wrapping or recording. */
  public Timer animalCreateTimer() {
    return animalCreateTimer;
  }

  // ─── Appointments ────────────────────────────────────────────────────

  /** Increments the appointment created counter. */
  public void recordAppointmentCreated() {
    appointmentsCreated.increment();
  }

  /** Increments the appointment confirmed counter. */
  public void recordAppointmentConfirmed() {
    appointmentsConfirmed.increment();
  }

  /** Increments the appointment completed counter. */
  public void recordAppointmentCompleted() {
    appointmentsCompleted.increment();
  }

  /** Increments the appointment cancelled counter. */
  public void recordAppointmentCancelled() {
    appointmentsCancelled.increment();
  }

  /** Increments the appointment rejected counter. */
  public void recordAppointmentRejected() {
    appointmentsRejected.increment();
  }

  /** Returns the appointment create Timer for wrapping or recording. */
  public Timer appointmentCreateTimer() {
    return appointmentCreateTimer;
  }

  // ─── AI Diagnostics ──────────────────────────────────────────────────

  /** Increments the AI diagnostic scan request counter. */
  public void recordAiDiagnosisRequest() {
    aiDiagnosisRequests.increment();
  }

  // ─── Notifications ───────────────────────────────────────────────────

  /** Increments the notification successfully dispatched counter. */
  public void recordNotificationSuccess() {
    notificationsSentSuccess.increment();
  }

  /** Increments the notification failed dispatch counter. */
  public void recordNotificationFailure() {
    notificationsSentFailure.increment();
  }

  /** Increments the notification queued (no device registered) counter. */
  public void recordNotificationQueued() {
    notificationsQueued.increment();
  }
}
