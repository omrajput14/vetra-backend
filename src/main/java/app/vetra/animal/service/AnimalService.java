package app.vetra.animal.service;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.dto.UpdateAnimalRequest;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.animal.specification.AnimalSpecification;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.ConflictException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.metrics.VetraMetrics;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business service for managing livestock animals with strict role authorization. */
@Service
public class AnimalService {

  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetraMetrics vetraMetrics;
  private final Tracer tracer;

  /** Constructor injection. */
  public AnimalService(
      AnimalRepository animalRepository,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetraMetrics vetraMetrics,
      Tracer tracer) {
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetraMetrics = vetraMetrics;
    this.tracer = tracer;
  }

  /** Creates a new animal record for the authenticated farmer. */
  @Transactional
  @CacheEvict(
      value = {CacheNames.DASHBOARD_FARMER, CacheNames.ANALYTICS},
      allEntries = true)
  public AnimalResponse createAnimal(String currentUserIdentifier, CreateAnimalRequest request) {
    User user = getUserByHeader(currentUserIdentifier);
    if (user.getRole() != UserRole.FARMER) {
      throw new UnauthorizedResourceAccessException(
          "Only farmers can register animals", "AUTH_006");
    }

    FarmerProfile farmer =
        farmerProfileRepository
            .findByUser(user)
            .orElseThrow(
                () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));

    if (animalRepository.existsByTagNumber(request.tagNumber())) {
      throw new ConflictException(
          "Tag number is already registered: " + request.tagNumber(), "ANIMAL_003");
    }
    if (request.qrCodeId() != null
        && !request.qrCodeId().isBlank()
        && animalRepository.existsByQrCodeId(request.qrCodeId())) {
      throw new ConflictException(
          "QR Code ID is already registered: " + request.qrCodeId(), "ANIMAL_003");
    }

    Animal animal =
        Animal.builder()
            .farmer(farmer)
            .animalName(request.animalName())
            .tagNumber(request.tagNumber())
            .qrCodeId(request.qrCodeId())
            .species(request.species())
            .breed(request.breed())
            .gender(request.gender())
            .birthDate(request.birthDate())
            .photoUrl(request.photoUrl())
            .build();

    animal = animalRepository.save(animal);
    vetraMetrics.recordAnimalRegistration();

    // Add low-cardinality business context to the active span.
    // species is an enum value — safe, bounded set, no PII.
    if (tracer.currentSpan() != null && request.species() != null) {
      tracer.currentSpan().tag("animal.species", request.species().name());
    }

    return mapToResponse(animal);
  }

  /** Retrieves an animal by ID with ownership verification. */
  @Transactional(readOnly = true)
  @Cacheable(
      value = CacheNames.ANIMALS,
      key = "T(app.vetra.infrastructure.cache.CacheKeys).animalKey(#animalId)")
  public AnimalResponse getAnimalById(String currentUserIdentifier, UUID animalId) {
    User user = getUserByHeader(currentUserIdentifier);
    Animal animal =
        animalRepository
            .findById(animalId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Animal not found with ID: " + animalId, "ANIMAL_001"));

    if (user.getRole() == UserRole.FARMER) {
      verifyFarmerOwnership(user, animal);
    }

    return mapToResponse(animal);
  }

  /** Lists animals with pagination based on user role. */
  @Transactional(readOnly = true)
  public Page<AnimalResponse> listAnimals(String currentUserIdentifier, Pageable pageable) {
    User user = getUserByHeader(currentUserIdentifier);

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer =
          farmerProfileRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      return animalRepository.findByFarmer(farmer, pageable).map(this::mapToResponse);
    }

    return animalRepository.findAll(pageable).map(this::mapToResponse);
  }

  /** Legacy list helper for non-paginated backward compatibility. */
  @Transactional(readOnly = true)
  public List<AnimalResponse> listAnimals(String currentUserIdentifier) {
    User user = getUserByHeader(currentUserIdentifier);

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer =
          farmerProfileRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      return animalRepository.findByFarmer(farmer).stream().map(this::mapToResponse).toList();
    }

    return animalRepository.findAll().stream().map(this::mapToResponse).toList();
  }

  /** Searches animals with optional filters using dynamic Criteria Specification. */
  @Transactional(readOnly = true)
  public List<AnimalResponse> searchAnimals(
      String currentUserIdentifier,
      String animalName,
      String tagNumber,
      String qrCodeId,
      Species species,
      String breed,
      AnimalGender gender) {

    User user = getUserByHeader(currentUserIdentifier);
    UUID farmerId = null;

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer =
          farmerProfileRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      farmerId = farmer.getId();
    }

    Specification<Animal> spec =
        AnimalSpecification.withFilters(
            farmerId, animalName, tagNumber, qrCodeId, species, breed, gender);

    return animalRepository.findAll(spec).stream().map(this::mapToResponse).toList();
  }

  /** Updates an existing animal record. */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(
            value = CacheNames.ANIMALS,
            key = "T(app.vetra.infrastructure.cache.CacheKeys).animalKey(#animalId)"),
        @CacheEvict(
            value = {CacheNames.DASHBOARD_FARMER, CacheNames.ANALYTICS},
            allEntries = true)
      })
  public AnimalResponse updateAnimal(
      String currentUserIdentifier, UUID animalId, UpdateAnimalRequest request) {
    User user = getUserByHeader(currentUserIdentifier);
    Animal animal =
        animalRepository
            .findById(animalId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Animal not found with ID: " + animalId, "ANIMAL_001"));

    if (user.getRole() == UserRole.FARMER) {
      verifyFarmerOwnership(user, animal);
    }

    if (!animal.getTagNumber().equalsIgnoreCase(request.tagNumber())
        && animalRepository.existsByTagNumber(request.tagNumber())) {
      throw new ConflictException(
          "Tag number is already registered: " + request.tagNumber(), "ANIMAL_003");
    }

    if (request.qrCodeId() != null
        && !request.qrCodeId().equalsIgnoreCase(animal.getQrCodeId())
        && animalRepository.existsByQrCodeId(request.qrCodeId())) {
      throw new ConflictException(
          "QR Code ID is already registered: " + request.qrCodeId(), "ANIMAL_003");
    }

    animal.setAnimalName(request.animalName());
    animal.setTagNumber(request.tagNumber());
    animal.setQrCodeId(request.qrCodeId());
    animal.setSpecies(request.species());
    animal.setBreed(request.breed());
    animal.setGender(request.gender());
    animal.setBirthDate(request.birthDate());
    animal.setPhotoUrl(request.photoUrl());

    animal = animalRepository.save(animal);
    return mapToResponse(animal);
  }

  /** Deletes an animal by ID. */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(
            value = CacheNames.ANIMALS,
            key = "T(app.vetra.infrastructure.cache.CacheKeys).animalKey(#animalId)"),
        @CacheEvict(
            value = {CacheNames.DASHBOARD_FARMER, CacheNames.ANALYTICS},
            allEntries = true)
      })
  public void deleteAnimal(String currentUserIdentifier, UUID animalId) {
    User user = getUserByHeader(currentUserIdentifier);
    Animal animal =
        animalRepository
            .findById(animalId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Animal not found with ID: " + animalId, "ANIMAL_001"));

    if (user.getRole() == UserRole.FARMER) {
      verifyFarmerOwnership(user, animal);
    }

    animalRepository.delete(animal);
  }

  private User getUserByHeader(String identifier) {
    return userRepository
        .findByIdentifier(identifier)
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }

  private void verifyFarmerOwnership(User user, Animal animal) {
    FarmerProfile farmer =
        farmerProfileRepository
            .findByUser(user)
            .orElseThrow(
                () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
    if (!animal.getFarmer().getId().equals(farmer.getId())) {
      throw new UnauthorizedResourceAccessException(
          "Access denied: You do not own this animal record", "ANIMAL_002");
    }
  }

  private AnimalResponse mapToResponse(Animal a) {
    return new AnimalResponse(
        a.getId(),
        a.getFarmer().getId(),
        a.getFarmer().getFullName(),
        a.getAnimalName(),
        a.getTagNumber(),
        a.getQrCodeId(),
        a.getSpecies(),
        a.getBreed(),
        a.getGender(),
        a.getBirthDate(),
        a.getPhotoUrl(),
        a.getCreatedAt(),
        a.getUpdatedAt());
  }
}
