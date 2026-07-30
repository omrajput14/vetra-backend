package app.vetra.medicalrecord.repository;

import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for Electronic Veterinary Medical Record operations.
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {

  /** Finds all medical records for a specific animal ordered by clinical history date descending. */
  List<MedicalRecord> findByAnimalIdOrderByCreatedAtDesc(UUID animalId);

  /** Finds all medical records belonging to a farmer's registered livestock. */
  List<MedicalRecord> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

  /** Finds all medical records belonging to a farmer with pagination. */
  Page<MedicalRecord> findByFarmerId(UUID farmerId, Pageable pageable);

  /** Finds all medical records created by a specific veterinarian. */
  List<MedicalRecord> findByVeterinarianIdOrderByCreatedAtDesc(UUID veterinarianId);

  /** Finds all medical records created by a veterinarian with pagination. */
  Page<MedicalRecord> findByVeterinarianId(UUID veterinarianId, Pageable pageable);

  /** Finds the medical record associated with a given appointment ID. */
  Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);

  /** Checks if a medical record has already been created for an appointment. */
  boolean existsByAppointmentId(UUID appointmentId);

  /** Counts total medical records issued by a veterinarian. */
  long countByVeterinarianId(UUID veterinarianId);
}
