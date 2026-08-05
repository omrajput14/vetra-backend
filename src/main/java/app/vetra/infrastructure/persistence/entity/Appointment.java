package app.vetra.infrastructure.persistence.entity;

import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.VisitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Scheduled consultation or checkup appointment. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_id", nullable = false)
  private FarmerProfile farmer;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "veterinarian_id", nullable = false)
  private VetProfile veterinarian;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @NotNull
  @Column(name = "appointment_date", nullable = false)
  private LocalDate appointmentDate;

  @NotNull
  @Column(name = "appointment_time", nullable = false)
  private LocalTime appointmentTime;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "visit_type", nullable = false, length = 50)
  private VisitType visitType;

  @NotNull
  @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
  private String reason;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private AppointmentStatus status;

  @Column(name = "veterinarian_notes", columnDefinition = "TEXT")
  private String veterinarianNotes;

  @Column(name = "cancellation_reason", columnDefinition = "TEXT")
  private String cancellationReason;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
