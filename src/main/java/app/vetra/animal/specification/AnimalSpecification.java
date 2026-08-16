package app.vetra.animal.specification;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spring Data JPA Specification builder for dynamic multi-filter animal queries.
 *
 * <p>Dynamically constructs Criteria API predicates ONLY for non-null and non-blank search
 * criteria. This eliminates static JPQL '(:param IS NULL OR a.field = :param)' expressions,
 * preventing PostgreSQL prepared statement parameter type deduction errors (PSQLException).
 */
public final class AnimalSpecification {

  private AnimalSpecification() {}

  /**
   * Builds a dynamic Specification with predicates for only non-null search criteria.
   *
   * @param farmerId optional farmer profile UUID
   * @param animalName optional animal name filter (case-insensitive substring)
   * @param tagNumber optional ear tag number filter (case-insensitive substring)
   * @param qrCodeId optional QR code ID filter (case-insensitive substring)
   * @param species optional species enum filter
   * @param breed optional breed filter (case-insensitive substring)
   * @param gender optional animal gender enum filter
   * @return JPA Specification for Animal entity
   */
  public static Specification<Animal> withFilters(
      UUID farmerId,
      String animalName,
      String tagNumber,
      String qrCodeId,
      Species species,
      String breed,
      AnimalGender gender) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (farmerId != null) {
        predicates.add(cb.equal(root.get("farmer").get("id"), farmerId));
      }
      if (animalName != null && !animalName.isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("animalName")), "%" + animalName.trim().toLowerCase() + "%"));
      }
      if (tagNumber != null && !tagNumber.isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("tagNumber")), "%" + tagNumber.trim().toLowerCase() + "%"));
      }
      if (qrCodeId != null && !qrCodeId.isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("qrCodeId")), "%" + qrCodeId.trim().toLowerCase() + "%"));
      }
      if (species != null) {
        predicates.add(cb.equal(root.get("species"), species));
      }
      if (breed != null && !breed.isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("breed")), "%" + breed.trim().toLowerCase() + "%"));
      }
      if (gender != null) {
        predicates.add(cb.equal(root.get("gender"), gender));
      }

      if (predicates.isEmpty()) {
        return cb.conjunction();
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
