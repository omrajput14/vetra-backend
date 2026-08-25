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
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.HealthRecordSource;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.LocalDateTime;
import java.util.List;
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

  /** Constructor injection. */
  public AnimalHealthRecordServiceImpl(
      AnimalHealthRecordRepository healthRecordRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      VetProfileRepository vetProfileRepository) {
    this.healthRecordRepository = healthRecordRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.vetProfileRepository = vetProfileRepository;
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

    return healthRecordRepository.findByAnimalIdOrderByRecordedAtDesc(animalId).stream()
        .map(AnimalHealthRecordDto::fromEntity)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AnimalHealthStatusDto getLatestHealthStatus(String userIdentifier, UUID animalId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = getAnimal(animalId);
    validateAccess(user, animal);

    Optional<AnimalHealthRecord> latestOpt =
        healthRecordRepository.findFirstByAnimalIdOrderByRecordedAtDesc(animalId);

    if (latestOpt.isEmpty()) {
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

    AnimalHealthRecord record = latestOpt.get();
    String status = "ACTIVE";
    String summary = record.getTitle();

    if (record.getRecordType() == HealthRecordType.TREATMENT) {
      status = "TREATMENT_IN_PROGRESS";
      summary = "Active Treatment: " + (record.getTreatment() != null ? record.getTreatment() : record.getTitle());
    } else if (record.getRecordType() == HealthRecordType.DIAGNOSIS) {
      status = "ATTENTION_REQUIRED";
      summary = "Confirmed Diagnosis: " + (record.getDiagnosis() != null ? record.getDiagnosis() : record.getTitle());
    } else if (record.getRecordType() == HealthRecordType.VACCINATION) {
      status = "PROTECTED";
      summary = "Vaccinated: " + record.getTitle();
    } else if (record.getRecordType() == HealthRecordType.AI_SCREENING) {
      status = "AI_SCREENED";
      summary = "AI Screening: " + record.getTitle();
    }

    return new AnimalHealthStatusDto(
        animalId,
        status,
        summary,
        record.getRecordType(),
        record.getSource(),
        record.getRecordedAt(),
        record.getDiagnosis(),
        record.getTreatment());
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
