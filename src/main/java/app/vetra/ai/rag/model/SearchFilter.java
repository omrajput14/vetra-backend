package app.vetra.ai.rag.model;

import java.util.Map;

/**
 * Filter criteria for targeted vector search in the veterinary knowledge base.
 *
 * @param species target animal species (e.g. CATTLE, POULTRY, SHEEP)
 * @param diseaseCategory clinical disease classification (e.g. INFECTIOUS, METABOLIC)
 * @param documentType document classification (e.g. PROTOCOL, ENCYCLOPEDIA, RESEARCH)
 * @param source trusted source or publication (e.g. FAO, WOAH, VETRA_CLINICAL)
 * @param customFilters optional arbitrary metadata filters
 */
public record SearchFilter(
    String species,
    String diseaseCategory,
    String documentType,
    String source,
    Map<String, String> customFilters) {

  /** Creates an empty filter that matches all documents. */
  public static SearchFilter empty() {
    return new SearchFilter(null, null, null, null, Map.of());
  }

  /** Creates a filter for a specific animal species. */
  public static SearchFilter ofSpecies(String species) {
    return new SearchFilter(species, null, null, null, Map.of());
  }

  /** Creates a filter for species and disease category. */
  public static SearchFilter of(String species, String diseaseCategory) {
    return new SearchFilter(species, diseaseCategory, null, null, Map.of());
  }
}
