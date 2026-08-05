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
}
