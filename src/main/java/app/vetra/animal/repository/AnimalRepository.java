package app.vetra.animal.repository;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access repository for Animal entity. */
public interface AnimalRepository extends JpaRepository<Animal, UUID> {

  /** Finds all animals owned by farmer (non-paginated). */
  List<Animal> findByFarmer(FarmerProfile farmer);

  /** Finds all animals owned by farmer with pagination. */
  Page<Animal> findByFarmer(FarmerProfile farmer, Pageable pageable);

  /** Counts total animals owned by farmer. */
  long countByFarmer(FarmerProfile farmer);

  /** Finds all animals owned by farmer id. */
  List<Animal> findByFarmerId(UUID farmerId);

  /** Finds animal by tag number. */
  Optional<Animal> findByTagNumber(String tagNumber);

  /** Finds animal by QR code ID. */
  Optional<Animal> findByQrCodeId(String qrCodeId);

  /** Checks if tag number exists. */
  boolean existsByTagNumber(String tagNumber);

  /** Checks if QR code ID exists. */
  boolean existsByQrCodeId(String qrCodeId);

  /** Search query filter for animals including animalName. */
  @Query(
      "SELECT a FROM Animal a WHERE "
          + "(:farmerId IS NULL OR a.farmer.id = :farmerId) AND "
          + "(:animalName IS NULL OR LOWER(a.animalName) LIKE LOWER(CONCAT('%', :animalName, '%'))) AND "
          + "(:tagNumber IS NULL OR LOWER(a.tagNumber) LIKE LOWER(CONCAT('%', :tagNumber, '%'))) AND "
          + "(:qrCodeId IS NULL OR LOWER(a.qrCodeId) LIKE LOWER(CONCAT('%', :qrCodeId, '%'))) AND "
          + "(:species IS NULL OR a.species = :species) AND "
          + "(:breed IS NULL OR LOWER(a.breed) LIKE LOWER(CONCAT('%', :breed, '%'))) AND "
          + "(:gender IS NULL OR a.gender = :gender)")
  List<Animal> searchAnimals(
      @Param("farmerId") UUID farmerId,
      @Param("animalName") String animalName,
      @Param("tagNumber") String tagNumber,
      @Param("qrCodeId") String qrCodeId,
      @Param("species") Species species,
      @Param("breed") String breed,
      @Param("gender") AnimalGender gender);
}
