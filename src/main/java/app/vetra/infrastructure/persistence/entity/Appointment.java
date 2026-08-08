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
  @Builder.Default
  private Long version = 0L;

  public FarmerProfile getFarmer() {
    return farmer;
  }

  public void setFarmer(FarmerProfile farmer) {
    this.farmer = farmer;
  }

  public VetProfile getVeterinarian() {
    return veterinarian;
  }

  public void setVeterinarian(VetProfile veterinarian) {
    this.veterinarian = veterinarian;
  }

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public LocalDate getAppointmentDate() {
    return appointmentDate;
  }

  public void setAppointmentDate(LocalDate appointmentDate) {
    this.appointmentDate = appointmentDate;
  }

  public LocalTime getAppointmentTime() {
    return appointmentTime;
  }

  public void setAppointmentTime(LocalTime appointmentTime) {
    this.appointmentTime = appointmentTime;
  }

  public VisitType getVisitType() {
    return visitType;
  }

  public void setVisitType(VisitType visitType) {
    this.visitType = visitType;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(AppointmentStatus status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getVeterinarianNotes() {
    return veterinarianNotes;
  }

  public void setVeterinarianNotes(String veterinarianNotes) {
    this.veterinarianNotes = veterinarianNotes;
  }

  public String getNotes() {
    return veterinarianNotes;
  }

  public void setNotes(String notes) {
    this.veterinarianNotes = notes;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public static AppointmentBuilder builder() {
    return new AppointmentBuilder();
  }

  public static class AppointmentBuilder {
    private FarmerProfile farmer;
    private VetProfile veterinarian;
    private Animal animal;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private VisitType visitType;
    private String reason;
    private AppointmentStatus status;
    private String veterinarianNotes;
    private String cancellationReason;
    private Long version = 0L;

    public AppointmentBuilder farmer(FarmerProfile farmer) {
      this.farmer = farmer;
      return this;
    }

    public AppointmentBuilder veterinarian(VetProfile veterinarian) {
      this.veterinarian = veterinarian;
      return this;
    }

    public AppointmentBuilder animal(Animal animal) {
      this.animal = animal;
      return this;
    }

    public AppointmentBuilder appointmentDate(LocalDate appointmentDate) {
      this.appointmentDate = appointmentDate;
      return this;
    }

    public AppointmentBuilder appointmentTime(LocalTime appointmentTime) {
      this.appointmentTime = appointmentTime;
      return this;
    }

    public AppointmentBuilder visitType(VisitType visitType) {
      this.visitType = visitType;
      return this;
    }

    public AppointmentBuilder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public AppointmentBuilder status(AppointmentStatus status) {
      this.status = status;
      return this;
    }

    public AppointmentBuilder veterinarianNotes(String veterinarianNotes) {
      this.veterinarianNotes = veterinarianNotes;
      return this;
    }

    public AppointmentBuilder cancellationReason(String cancellationReason) {
      this.cancellationReason = cancellationReason;
      return this;
    }

    public AppointmentBuilder version(Long version) {
      this.version = version;
      return this;
    }

    public Appointment build() {
      Appointment appointment = new Appointment();
      appointment.setFarmer(this.farmer);
      appointment.setVeterinarian(this.veterinarian);
      appointment.setAnimal(this.animal);
      appointment.setAppointmentDate(this.appointmentDate);
      appointment.setAppointmentTime(this.appointmentTime);
      appointment.setVisitType(this.visitType);
      appointment.setReason(this.reason);
      appointment.setStatus(this.status);
      appointment.setVeterinarianNotes(this.veterinarianNotes);
      appointment.setCancellationReason(this.cancellationReason);
      appointment.setVersion(this.version);
      return appointment;
    }
  }
}
