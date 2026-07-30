package app.vetra.appointment.repository;

import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Appointment entities.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

  /** Finds all appointments for a farmer ordered by date and time descending. */
  List<Appointment> findByFarmerOrderByAppointmentDateDescAppointmentTimeDesc(FarmerProfile farmer);

  /** Finds all appointments for a farmer with pagination. */
  Page<Appointment> findByFarmer(FarmerProfile farmer, Pageable pageable);

  /** Finds appointments for a farmer filtered by status. */
  List<Appointment> findByFarmerAndStatusOrderByAppointmentDateDescAppointmentTimeDesc(
      FarmerProfile farmer, AppointmentStatus status);

  /** Finds all appointments for a veterinarian ordered by date and time descending. */
  List<Appointment> findByVeterinarianOrderByAppointmentDateDescAppointmentTimeDesc(
      VetProfile veterinarian);

  /** Finds all appointments for a veterinarian with pagination. */
  Page<Appointment> findByVeterinarian(VetProfile veterinarian, Pageable pageable);

  /** Finds appointments for a veterinarian filtered by status. */
  List<Appointment> findByVeterinarianAndStatusOrderByAppointmentDateDescAppointmentTimeDesc(
      VetProfile veterinarian, AppointmentStatus status);

  /** Counts appointments for a farmer by status. */
  long countByFarmerAndStatus(FarmerProfile farmer, AppointmentStatus status);

  /** Counts appointments for a veterinarian by status. */
  long countByVeterinarianAndStatus(VetProfile veterinarian, AppointmentStatus status);

  /** Finds appointment by ID and farmer. */
  Optional<Appointment> findByIdAndFarmer(UUID id, FarmerProfile farmer);

  /** Finds appointment by ID and veterinarian. */
  Optional<Appointment> findByIdAndVeterinarian(UUID id, VetProfile veterinarian);
}
