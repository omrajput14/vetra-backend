package app.vetra.animal.service.impl;

import app.vetra.animal.dto.AnimalHealthRecordDto;
import app.vetra.animal.dto.AnimalHealthStatusDto;
import app.vetra.animal.dto.CreateHealthRecordRequest;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.animal.service.AnimalHealthRecordService;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business service implementing Animal Medical Passport and Lifetime Health Timeline workflows. */
@Service
public class AnimalHealthRecordServiceImpl implements AnimalHealthRecordService {

  private final AnimalHealthRecordRepository healthRecordRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final VetProfileRepository vetProfileRepository;
  private final MedicalRecordRepository medicalRecordRepository;

  /** Constructor injection. */
  public AnimalHealthRecordServiceImpl(
      AnimalHealthRecordRepository healthRecordRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      VetProfileRepository vetProfileRepository,
      MedicalRecordRepository medicalRecordRepository) {
    this.healthRecordRepository = healthRecordRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.medicalRecordRepository = medicalRecordRepository;
  }

  @Override
  @Transactional
  public AnimalHealthRecordDto createRecord(
      String userIdentifier, UUID animalId, CreateHealthRecordRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = getAnimal(animalId);
    validateAccess(user, animal);

    HealthRecordSource source = request.source();
    UUID vetId = request.veterinarianId();
    String vetName = request.veterinarianName();

    if (user.getRole() == UserRole.VETERINARIAN) {
      if (source == null) {
        source = HealthRecordSource.VETERINARIAN;
      }
      Optional<VetProfile> vetProfileOpt = vetProfileRepository.findByUser(user);
      if (vetProfileOpt.isPresent()) {
        VetProfile vp = vetProfileOpt.get();
        if (vetId == null) {
          vetId = vp.getId();
        }
        if (vetName == null || vetName.isBlank()) {
          vetName = vp.getFullName();
        }
      }
    } else if (user.getRole() == UserRole.FARMER) {
      if (source == null) {
        source = HealthRecordSource.FARMER;
      }
    } else if (source == null) {
      source = HealthRecordSource.SYSTEM;
    }

    AnimalHealthRecord record =
        AnimalHealthRecord.builder()
            .animal(animal)
            .recordType(request.recordType())
            .source(source)
            .title(request.title())
            .description(request.description())
            .symptoms(request.symptoms())
            .diagnosis(request.diagnosis())
            .treatment(request.treatment())
            .veterinarianId(vetId)
            .veterinarianName(vetName)
            .documentUrl(request.documentUrl())
            .vaccineName(request.vaccineName())
            .nextDueDate(request.nextDueDate())
            .batchNumber(request.batchNumber())
            .recordedAt(request.recordedAt() != null ? request.recordedAt() : LocalDateTime.now())
            .build();

    AnimalHealthRecord saved = healthRecordRepository.save(record);
    return AnimalHealthRecordDto.fromEntity(saved);
  }

  @Override
  @Transactional
  public AnimalHealthRecordDto createInternalRecord(
      UUID animalId, CreateHealthRecordRequest request) {
    Animal animal = getAnimal(animalId);

    AnimalHealthRecord record =
        AnimalHealthRecord.builder()
            .animal(animal)
            .recordType(request.recordType())
            .source(request.source() != null ? request.source() : HealthRecordSource.AI_ADVISOR)
            .title(request.title())
            .description(request.description())
            .symptoms(request.symptoms())
            .diagnosis(request.diagnosis())
            .treatment(request.treatment())
            .veterinarianId(request.veterinarianId())
            .veterinarianName(request.veterinarianName())
            .documentUrl(request.documentUrl())
            .vaccineName(request.vaccineName())
            .nextDueDate(request.nextDueDate())
            .batchNumber(request.batchNumber())
            .recordedAt(request.recordedAt() != null ? request.recordedAt() : LocalDateTime.now())
            .build();

    AnimalHealthRecord saved = healthRecordRepository.save(record);
    return AnimalHealthRecordDto.fromEntity(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AnimalHealthRecordDto> getAnimalTimeline(String userIdentifier, UUID animalId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = getAnimal(animalId);
    validateAccess(user, animal);

    List<AnimalHealthRecord> healthRecords =
        healthRecordRepository.findByAnimalIdOrderByRecordedAtDesc(animalId);
    List<MedicalRecord> medicalRecords =
        medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(animalId);

    List<AnimalHealthRecordDto> timeline = new ArrayList<>();
    for (AnimalHealthRecord hr : healthRecords) {
      timeline.add(AnimalHealthRecordDto.fromEntity(hr));
    }

    for (MedicalRecord mr : medicalRecords) {
      if (!isAlreadyProjected(mr, healthRecords)) {
        timeline.add(projectLegacyMedicalRecord(mr));
      }
    }

    timeline.sort((a, b) -> b.recordedAt().compareTo(a.recordedAt()));
    return timeline;
  }

  private boolean isAlreadyProjected(MedicalRecord mr, List<AnimalHealthRecord> healthRecords) {
    for (AnimalHealthRecord hr : healthRecords) {
      if (matchesDeterministic(hr, mr) || matchesLegacyFallback(hr, mr)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesDeterministic(AnimalHealthRecord hr, MedicalRecord mr) {
    if (hr.getMedicalRecordId() != null && hr.getMedicalRecordId().equals(mr.getId())) {
      return true;
    }
    return hr.getAppointmentId() != null
        && mr.getAppointment() != null
        && hr.getAppointmentId().equals(mr.getAppointment().getId());
  }

  private boolean matchesLegacyFallback(AnimalHealthRecord hr, MedicalRecord mr) {
    if (hr.getRecordType() != HealthRecordType.VET_CONSULTATION
        || hr.getVeterinarianId() == null
        || mr.getVeterinarian() == null
        || !hr.getVeterinarianId().equals(mr.getVeterinarian().getId())
        || !Objects.equals(hr.getDiagnosis(), mr.getDiagnosis())) {
      return false;
    }

    LocalDateTime mrTime = mr.getCreatedAt() != null ? mr.getCreatedAt().toLocalDateTime() : null;
    if (mrTime == null || hr.getRecordedAt() == null) {
      return false;
    }

    long diffSeconds = Math.abs(java.time.Duration.between(hr.getRecordedAt(), mrTime).toSeconds());
    return diffSeconds < 120;
  }

  private AnimalHealthRecordDto projectLegacyMedicalRecord(MedicalRecord mr) {
    String title = "Clinical Consultation: " + mr.getDiagnosis();
    if (title.length() > 200) {
      title = title.substring(0, 197) + "...";
    }

    String combinedTreatment = mr.getTreatment();
    if (mr.getPrescription() != null && !mr.getPrescription().isBlank()) {
      combinedTreatment = combinedTreatment + "\nPrescription: " + mr.getPrescription();
    }

    LocalDateTime recordedAt =
        mr.getCreatedAt() != null ? mr.getCreatedAt().toLocalDateTime() : LocalDateTime.now();

    return new AnimalHealthRecordDto(
        mr.getId(),
        mr.getAnimal().getId(),
        HealthRecordType.VET_CONSULTATION,
        HealthRecordSource.VETERINARIAN,
        title,
        mr.getNotes(),
        mr.getSymptoms(),
        mr.getDiagnosis(),
        combinedTreatment,
        mr.getVeterinarian() != null ? mr.getVeterinarian().getId() : null,
        mr.getVeterinarian() != null ? mr.getVeterinarian().getFullName() : null,
        null,
        null,
        mr.getFollowUpDate(),
        null,
        mr.getId(),
        mr.getAppointment() != null ? mr.getAppointment().getId() : null,
        recordedAt,
        recordedAt);
  }

  @Override
  @Transactional(readOnly = true)
  public AnimalHealthStatusDto getLatestHealthStatus(String userIdentifier, UUID animalId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = getAnimal(animalId);
    validateAccess(user, animal);

    List<AnimalHealthRecordDto> timeline = getAnimalTimeline(userIdentifier, animalId);

    if (timeline.isEmpty()) {
      return new AnimalHealthStatusDto(
          animalId,
          "HEALTHY",
          "No adverse health events recorded. Animal is in healthy status.",
          null,
          null,
          null,
          null,
          null);
    }

    AnimalHealthRecordDto record = timeline.get(0);
    String status = "ACTIVE";
    String summary = record.title();

    if (record.recordType() == HealthRecordType.TREATMENT) {
      status = "TREATMENT_IN_PROGRESS";
      summary =
          "Active Treatment: "
              + (record.treatment() != null ? record.treatment() : record.title());
    } else if (record.recordType() == HealthRecordType.DIAGNOSIS) {
      status = "ATTENTION_REQUIRED";
      summary =
          "Confirmed Diagnosis: "
              + (record.diagnosis() != null ? record.diagnosis() : record.title());
    } else if (record.recordType() == HealthRecordType.VACCINATION) {
      status = "PROTECTED";
      summary = "Vaccinated: " + record.title();
    } else if (record.recordType() == HealthRecordType.AI_SCREENING) {
      status = "AI_SCREENED";
      summary = "AI Screening: " + record.title();
    }

    return new AnimalHealthStatusDto(
        animalId,
        status,
        summary,
        record.recordType(),
        record.source(),
        record.recordedAt(),
        record.diagnosis(),
        record.treatment());
  }

  private void validateAccess(User user, Animal animal) {
    if (user.getRole() == UserRole.FARMER) {
      if (animal.getFarmer() == null
          || animal.getFarmer().getUser() == null
          || !animal.getFarmer().getUser().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException(
            "Access denied. You do not own this livestock animal.", "AUTH_003");
      }
    }
  }

  private Animal getAnimal(UUID animalId) {
    return animalRepository
        .findById(animalId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Animal not found with ID: " + animalId, "ANIMAL_001"));
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository
        .findByIdentifier(identifier)
        .or(() -> userRepository.findByEmail(identifier))
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "User account not found for: " + identifier, "USER_004"));
  }
}
