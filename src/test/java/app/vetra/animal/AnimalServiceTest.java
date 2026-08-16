package app.vetra.animal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.dto.UpdateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Integration and unit tests for AnimalService. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_animal_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=false",
      "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
      "vetra.jwt.expiration-ms=86400000",
      "vetra.jwt.refresh-expiration-ms=604800000",
      "vetra.cors.allowed-origins=http://localhost:3000",
      "vetra.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
      "vetra.cors.allowed-headers=*",
      "vetra.cors.allow-credentials=true",
      "vetra.cors.max-age=3600",
      "vetra.aws.region=ap-south-1",
      "vetra.aws.credentials.access-key=test-key",
      "vetra.aws.credentials.secret-key=test-secret",
      "vetra.aws.s3.bucket-name=vetra-test-bucket",
      "vetra.aws.s3.presigned-url-expiry-minutes=15",
    })
class AnimalServiceTest {

  @Autowired private AnimalService animalService;

  @Autowired private AuthService authService;

  @Test
  void testAnimalLifecycle() {
    FarmerRegisterRequest farmerReq =
        new FarmerRegisterRequest(
            "animalfarmer@vetra.app",
            "+1555019888",
            "pass123",
            "Animal Owner",
            "Green Farm",
            "Village",
            "District",
            "State",
            12.0,
            56.0,
            10);
    authService.registerFarmer(farmerReq);

    CreateAnimalRequest createReq =
        new CreateAnimalRequest(
            "Bessie",
            "TAG-1001",
            "QR-1001",
            Species.CATTLE,
            "Holstein",
            AnimalGender.FEMALE,
            LocalDate.of(2022, 5, 10),
            "https://vetra.app/photos/cow1.png");

    AnimalResponse animal = animalService.createAnimal("animalfarmer@vetra.app", createReq);
    assertNotNull(animal.id());
    assertEquals("Bessie", animal.animalName());
    assertEquals("TAG-1001", animal.tagNumber());
    assertEquals(Species.CATTLE, animal.species());

    List<AnimalResponse> list = animalService.listAnimals("animalfarmer@vetra.app");
    assertEquals(1, list.size());

    UpdateAnimalRequest updateReq =
        new UpdateAnimalRequest(
            "Bessie Supreme",
            "TAG-1001-UPDATED",
            "QR-1001",
            Species.CATTLE,
            "Holstein Friesian",
            AnimalGender.FEMALE,
            LocalDate.of(2022, 5, 10),
            "https://vetra.app/photos/cow1.png");

    AnimalResponse updated =
        animalService.updateAnimal("animalfarmer@vetra.app", animal.id(), updateReq);
    assertEquals("Bessie Supreme", updated.animalName());
    assertEquals("TAG-1001-UPDATED", updated.tagNumber());

    List<AnimalResponse> searchResults =
        animalService.searchAnimals(
            "animalfarmer@vetra.app", "Bessie", "TAG-1001", null, Species.CATTLE, null, null);
    assertFalse(searchResults.isEmpty());

    animalService.deleteAnimal("animalfarmer@vetra.app", animal.id());
    List<AnimalResponse> emptyList = animalService.listAnimals("animalfarmer@vetra.app");
    assertEquals(0, emptyList.size());
  }

  @Test
  void testAnimalSearchFilterPermutations() {
    String farmerEmail = "search_farmer@vetra.app";
    FarmerRegisterRequest farmerReq =
        new FarmerRegisterRequest(
            farmerEmail,
            "+1555099777",
            "pass123",
            "Search Farmer",
            "Multi Animal Farm",
            "Village A",
            "District B",
            "State C",
            15.0,
            75.0,
            4);
    authService.registerFarmer(farmerReq);

    // 1. Cattle Female Sahiwal
    animalService.createAnimal(
        farmerEmail,
        new CreateAnimalRequest(
            "Gauri",
            "TAG-CATTLE-01",
            "QR-CATTLE-01",
            Species.CATTLE,
            "Sahiwal",
            AnimalGender.FEMALE,
            LocalDate.of(2023, 1, 15),
            null));

    // 2. Cattle Male Gir
    animalService.createAnimal(
        farmerEmail,
        new CreateAnimalRequest(
            "Nandi",
            "TAG-CATTLE-02",
            "QR-CATTLE-02",
            Species.CATTLE,
            "Gir",
            AnimalGender.MALE,
            LocalDate.of(2022, 6, 20),
            null));

    // 3. Goat Female Jamnapari
    animalService.createAnimal(
        farmerEmail,
        new CreateAnimalRequest(
            "Munni",
            "TAG-GOAT-01",
            "QR-GOAT-01",
            Species.GOAT,
            "Jamnapari",
            AnimalGender.FEMALE,
            LocalDate.of(2024, 2, 10),
            null));

    // 4. Goat Male Boer
    animalService.createAnimal(
        farmerEmail,
        new CreateAnimalRequest(
            "Ramu",
            "TAG-GOAT-02",
            "QR-GOAT-02",
            Species.GOAT,
            "Boer",
            AnimalGender.MALE,
            LocalDate.of(2023, 11, 5),
            null));

    // Test Permutation 1: only tagNumber
    List<AnimalResponse> resOnlyTag =
        animalService.searchAnimals(farmerEmail, null, "TAG-CATTLE-01", null, null, null, null);
    assertEquals(1, resOnlyTag.size());
    assertEquals("TAG-CATTLE-01", resOnlyTag.get(0).tagNumber());
    assertEquals("Gauri", resOnlyTag.get(0).animalName());

    // Test Permutation 2: tagNumber + species
    List<AnimalResponse> resTagAndSpecies =
        animalService.searchAnimals(
            farmerEmail, null, "TAG-CATTLE", null, Species.CATTLE, null, null);
    assertEquals(2, resTagAndSpecies.size());

    List<AnimalResponse> resTagAndMismatchedSpecies =
        animalService.searchAnimals(
            farmerEmail, null, "TAG-CATTLE-01", null, Species.GOAT, null, null);
    assertEquals(0, resTagAndMismatchedSpecies.size());

    // Test Permutation 3: tagNumber + gender
    List<AnimalResponse> resTagAndGender =
        animalService.searchAnimals(
            farmerEmail, null, "TAG-GOAT", null, null, null, AnimalGender.FEMALE);
    assertEquals(1, resTagAndGender.size());
    assertEquals("Munni", resTagAndGender.get(0).animalName());

    // Test Permutation 4: species + gender
    List<AnimalResponse> resSpeciesAndGender =
        animalService.searchAnimals(
            farmerEmail, null, null, null, Species.CATTLE, null, AnimalGender.FEMALE);
    assertEquals(1, resSpeciesAndGender.size());
    assertEquals("Gauri", resSpeciesAndGender.get(0).animalName());

    List<AnimalResponse> resAllMaleCattle =
        animalService.searchAnimals(
            farmerEmail, null, null, null, Species.CATTLE, null, AnimalGender.MALE);
    assertEquals(1, resAllMaleCattle.size());
    assertEquals("Nandi", resAllMaleCattle.get(0).animalName());

    // Test Permutation 5: all filters provided
    List<AnimalResponse> resAllFilters =
        animalService.searchAnimals(
            farmerEmail,
            "Gauri",
            "TAG-CATTLE-01",
            "QR-CATTLE-01",
            Species.CATTLE,
            "Sahiwal",
            AnimalGender.FEMALE);
    assertEquals(1, resAllFilters.size());
    assertEquals("Gauri", resAllFilters.get(0).animalName());

    // Test Permutation 6: all optional filters omitted (returns all registered animals for farmer)
    List<AnimalResponse> resAllOmitted =
        animalService.searchAnimals(farmerEmail, null, null, null, null, null, null);
    assertEquals(4, resAllOmitted.size());
  }
}
