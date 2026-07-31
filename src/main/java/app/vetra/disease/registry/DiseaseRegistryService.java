package app.vetra.disease.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Service managing the disease taxonomy and epidemiological metadata catalog.
 */
@Service
public class DiseaseRegistryService {

  private final Map<String, DiseaseMetadata> registry = new ConcurrentHashMap<>();

  /** Initializing disease registry catalog. */
  public DiseaseRegistryService() {
    register(new DiseaseMetadata("Foot and Mouth Disease", "HIGH", false, true, "MEDIUM", 25.0, 3, 48));
    register(new DiseaseMetadata("Rabies", "CRITICAL", true, true, "VERY_HIGH", 50.0, 1, 24));
    register(new DiseaseMetadata("Brucellosis", "HIGH", true, true, "MEDIUM", 10.0, 5, 168));
    register(new DiseaseMetadata("Anthrax", "CRITICAL", true, true, "VERY_HIGH", 30.0, 1, 24));
    register(new DiseaseMetadata("Avian Influenza", "CRITICAL", true, true, "HIGH", 20.0, 2, 48));
    register(new DiseaseMetadata("African Swine Fever", "CRITICAL", false, true, "VERY_HIGH", 30.0, 2, 48));
    register(new DiseaseMetadata("Lumpy Skin Disease", "HIGH", false, true, "MEDIUM", 15.0, 3, 72));
    register(new DiseaseMetadata("Bovine Mastitis", "MEDIUM", false, false, "LOW", 10.0, 5, 72));
  }

  private void register(DiseaseMetadata metadata) {
    registry.put(metadata.diseaseName().toLowerCase(), metadata);
  }

  /** Retrieves all registered disease descriptors. */
  public List<DiseaseMetadata> getAllDiseases() {
    return List.copyOf(registry.values());
  }

  /** Finds disease metadata by disease name. */
  public Optional<DiseaseMetadata> getDiseaseByName(String diseaseName) {
    if (diseaseName == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(registry.get(diseaseName.trim().toLowerCase()));
  }
}
