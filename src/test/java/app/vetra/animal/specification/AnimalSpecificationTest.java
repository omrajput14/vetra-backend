package app.vetra.animal.specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/** Unit tests verifying that AnimalSpecification builds predicates dynamically. */
class AnimalSpecificationTest {

  private Root<Animal> root;
  private CriteriaQuery<?> query;
  private CriteriaBuilder cb;
  private Path<Object> farmerPath;
  private Path<Object> farmerIdPath;
  private Path<String> stringPath;
  private Path<Species> speciesPath;
  private Path<AnimalGender> genderPath;
  private Predicate dummyPredicate;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    root = mock(Root.class);
    query = mock(CriteriaQuery.class);
    cb = mock(CriteriaBuilder.class);
    farmerPath = mock(Path.class);
    farmerIdPath = mock(Path.class);
    stringPath = mock(Path.class);
    speciesPath = mock(Path.class);
    genderPath = mock(Path.class);
    dummyPredicate = mock(Predicate.class);

    when(root.get("farmer")).thenReturn(farmerPath);
    when(farmerPath.get("id")).thenReturn(farmerIdPath);
    when(root.<String>get(anyString())).thenReturn(stringPath);
    when(root.<Species>get("species")).thenReturn(speciesPath);
    when(root.<AnimalGender>get("gender")).thenReturn(genderPath);

    when(cb.equal(any(Expression.class), any())).thenReturn(dummyPredicate);
    when(cb.conjunction()).thenReturn(dummyPredicate);
    when(cb.and(any(Predicate[].class))).thenReturn(dummyPredicate);
    when(cb.lower(any(Expression.class))).thenReturn(stringPath);
    when(cb.like(any(Expression.class), anyString())).thenReturn(dummyPredicate);
  }

  @Test
  void testAllFiltersOmitted_createsConjunctionPredicate() {
    Specification<Animal> spec = AnimalSpecification.withFilters(null, null, null, null, null, null, null);
    Predicate result = spec.toPredicate(root, query, cb);

    assertNotNull(result);
    verify(cb).conjunction();
  }

  @Test
  void testOnlyTagNumber_createsSingleTagPredicate() {
    Specification<Animal> spec =
        AnimalSpecification.withFilters(null, null, "TAG-100", null, null, null, null);
    Predicate result = spec.toPredicate(root, query, cb);

    assertNotNull(result);
    verify(root).get("tagNumber");
    verify(root, never()).get("species");
    verify(root, never()).get("gender");
    verify(cb).like(stringPath, "%tag-100%");
  }

  @Test
  void testTagNumberAndSpecies_createsTagAndSpeciesPredicates() {
    Specification<Animal> spec =
        AnimalSpecification.withFilters(null, null, "TAG-100", null, Species.CATTLE, null, null);
    Predicate result = spec.toPredicate(root, query, cb);

    assertNotNull(result);
    verify(root).get("tagNumber");
    verify(root).get("species");
    verify(root, never()).get("gender");
    verify(cb).like(stringPath, "%tag-100%");
    verify(cb).equal(speciesPath, Species.CATTLE);
  }

  @Test
  void testSpeciesAndGender_createsEnumPredicatesOnly() {
    Specification<Animal> spec =
        AnimalSpecification.withFilters(null, null, null, null, Species.GOAT, null, AnimalGender.FEMALE);
    Predicate result = spec.toPredicate(root, query, cb);

    assertNotNull(result);
    verify(root).get("species");
    verify(root).get("gender");
    verify(root, never()).get("tagNumber");
    verify(cb).equal(speciesPath, Species.GOAT);
    verify(cb).equal(genderPath, AnimalGender.FEMALE);
  }

  @Test
  void testAllFiltersProvided_createsAllSevenPredicates() {
    UUID farmerId = UUID.randomUUID();

    Specification<Animal> spec =
        AnimalSpecification.withFilters(
            farmerId,
            "Lakshmi",
            "TAG-01",
            "QR-01",
            Species.CATTLE,
            "Sahiwal",
            AnimalGender.MALE);
    Predicate result = spec.toPredicate(root, query, cb);

    assertNotNull(result);
    verify(root).get("farmer");
    verify(root).get("animalName");
    verify(root).get("tagNumber");
    verify(root).get("qrCodeId");
    verify(root).get("species");
    verify(root).get("breed");
    verify(root).get("gender");
    verify(cb, times(1)).and(any(Predicate[].class));
  }
}
