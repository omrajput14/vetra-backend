package app.vetra.animal.repository;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access repository for Animal entity. */
public interface AnimalRepository
    extends JpaRepository<Animal, UUID>, JpaSpecificationExecutor<Animal> {

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
}
