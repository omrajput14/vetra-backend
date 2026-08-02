package app.vetra.medicalrecord.service;

import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ConflictException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.medicalrecord.dto.CreateMedicalRecordRequest;
import app.vetra.medicalrecord.dto.MedicalRecordResponse;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import app.vetra.infrastructure.cache.CacheNames;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic service for Electronic Veterinary Medical Records (EVMR).
 * Enforces strict immutability, state validation, and authorization boundaries.
 */
@Service
public class MedicalRecordService {

  private final MedicalRecordRepository medicalRecordRepository;
  private final AppointmentRepository appointmentRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;

  /** Constructor injection. */
  public MedicalRecordService(
      MedicalRecordRepository medicalRecordRepository,
      AppointmentRepository appointmentRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository) {
    this.medicalRecordRepository = medicalRecordRepository;
    this.appointmentRepository = appointmentRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
  }

  /**
   * Creates an Electronic Veterinary Medical Record for a completed appointment.
   * Only assigned veterinarians can issue medical records.
   */
  @Transactional
  @CacheEvict(
      value = {
          CacheNames.DASHBOARD_FARMER,
          CacheNames.DASHBOARD_VET,
          CacheNames.ANIMALS,
          CacheNames.ANALYTICS
      },
      allEntries = true)
  public MedicalRecordResponse createMedicalRecord(String userIdentifier, CreateMedicalRecordRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    if (user.getRole() != UserRole.VETERINARIAN) {
      throw new UnauthorizedResourceAccessException("Only veterinarians can create medical records", "AUTH_006");
    }

    VetProfile vetProfile = vetProfileRepository.findByUserId(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Veterinarian profile not found", "USER_004"));

    Appointment appointment = appointmentRepository.findById(request.appointmentId())
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.appointmentId(), "APPT_001"));

    if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
      throw new BusinessRuleException("Medical record can only be created for COMPLETED appointments", "APPT_008");
    }

    if (!appointment.getVeterinarian().getId().equals(vetProfile.getId())) {
      throw new UnauthorizedResourceAccessException("Veterinarians can only create medical records for appointments assigned to them", "MEDICAL_003");
    }

    if (medicalRecordRepository.existsByAppointmentId(request.appointmentId())) {
      throw new ConflictException("A medical record already exists for this appointment", "MEDICAL_004");
    }

    if (request.followUpDate() != null && request.followUpDate().isBefore(LocalDate.now())) {
      throw new BusinessRuleException("Follow-up date cannot be in the past", "APPT_007");
    }

    MedicalRecord record = MedicalRecord.builder()
        .appointment(appointment)
        .animal(appointment.getAnimal())
        .farmer(appointment.getFarmer())
        .veterinarian(vetProfile)
        .diagnosis(request.diagnosis().trim())
        .symptoms(request.symptoms() != null ? request.symptoms().trim() : null)
        .treatment(request.treatment().trim())
        .prescription(request.prescription() != null ? request.prescription().trim() : null)
        .weight(request.weight())
        .temperature(request.temperature())
        .followUpDate(request.followUpDate())
        .notes(request.notes() != null ? request.notes().trim() : null)
        .build();

    MedicalRecord saved = medicalRecordRepository.save(record);
    return MedicalRecordResponse.fromEntity(saved);
  }

  /** Retrieves a medical record by ID with authorization checks. */
  @Transactional(readOnly = true)
  @Cacheable(
      value = CacheNames.MEDICAL_RECORDS,
      key = "T(app.vetra.infrastructure.cache.CacheKeys).medicalRecordKey(#recordId)")
  public MedicalRecordResponse getMedicalRecordById(String userIdentifier, UUID recordId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    MedicalRecord record = medicalRecordRepository.findById(recordId)
        .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with ID: " + recordId, "MEDICAL_001"));

    validateRecordAccess(user, record);
    return MedicalRecordResponse.fromEntity(record);
  }

  /** Retrieves a medical record associated with an appointment ID. */
  @Transactional(readOnly = true)
  public MedicalRecordResponse getMedicalRecordByAppointmentId(String userIdentifier, UUID appointmentId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    MedicalRecord record = medicalRecordRepository.findByAppointmentId(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Medical record not found for appointment: " + appointmentId, "MEDICAL_001"));

    validateRecordAccess(user, record);
    return MedicalRecordResponse.fromEntity(record);
  }

  /** Retrieves full medical history for a specific animal. */
  @Transactional(readOnly = true)
  public List<MedicalRecordResponse> getAnimalMedicalHistory(String userIdentifier, UUID animalId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = animalRepository.findById(animalId)
        .orElseThrow(() -> new ResourceNotFoundException("Animal not found with ID: " + animalId, "ANIMAL_001"));

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUserId(user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      if (!animal.getFarmer().getId().equals(farmer.getId())) {
        throw new UnauthorizedResourceAccessException("Farmers can only view medical records belonging to their own animals", "MEDICAL_002");
      }
    }

    return medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(animalId).stream()
        .map(MedicalRecordResponse::fromEntity)
        .toList();
  }

  /** Lists medical records for current user (Farmer or Vet) non-paginated. */
  @Transactional(readOnly = true)
  public List<MedicalRecordResponse> listMedicalRecords(String userIdentifier) {
    User user = getUserByEmailOrPhone(userIdentifier);
    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUserId(user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      return medicalRecordRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream()
          .map(MedicalRecordResponse::fromEntity)
          .toList();
    } else {
      VetProfile vet = vetProfileRepository.findByUserId(user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Veterinarian profile not found", "USER_004"));
      return medicalRecordRepository.findByVeterinarianIdOrderByCreatedAtDesc(vet.getId()).stream()
          .map(MedicalRecordResponse::fromEntity)
          .toList();
    }
  }

  /** Lists medical records for current user with Pageable. */
  @Transactional(readOnly = true)
  public Page<MedicalRecordResponse> listMedicalRecords(String userIdentifier, Pageable pageable) {
    User user = getUserByEmailOrPhone(userIdentifier);
    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUserId(user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      return medicalRecordRepository.findByFarmerId(farmer.getId(), pageable).map(MedicalRecordResponse::fromEntity);
    } else {
      VetProfile vet = vetProfileRepository.findByUserId(user.getId())
          .orElseThrow(() -> new ResourceNotFoundException("Veterinarian profile not found", "USER_004"));
      return medicalRecordRepository.findByVeterinarianId(vet.getId(), pageable).map(MedicalRecordResponse::fromEntity);
    }
  }

  private void validateRecordAccess(User user, MedicalRecord record) {
    if (user.getRole() == UserRole.FARMER) {
      if (!record.getFarmer().getUser().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException("Farmers can only view medical records belonging to their own animals", "MEDICAL_002");
      }
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      if (!record.getVeterinarian().getUser().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException("Veterinarians can only view medical records created by them", "MEDICAL_003");
      }
    }
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository.findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
