package app.vetra.animal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.service.AnimalService;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.AnimalStatus;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.media.service.MediaStorageService;
import app.vetra.media.service.MediaStorageService.MediaResource;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class AnimalPhotoControllerTest {

  private AnimalService animalService;
  private MediaStorageService mediaStorageService;
  private AnimalPhotoController controller;
  private Principal principal;

  @BeforeEach
  void setUp() {
    animalService = mock(AnimalService.class);
    mediaStorageService = mock(MediaStorageService.class);
    controller = new AnimalPhotoController(animalService, mediaStorageService);
    principal = () -> "farmer@vetra.co.in";
  }

  @Test
  void testUploadPhotoSuccess() {
    UUID animalId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    Animal animal = new Animal();
    animal.setId(animalId);
    when(animalService.verifyAndGetAnimalForPhotoAccess("farmer@vetra.co.in", animalId)).thenReturn(animal);

    AnimalResponse response = new AnimalResponse(
        animalId, UUID.randomUUID(), "Ramesh", "Gauri", "TAG-101", "QR-101",
        Species.CATTLE, "Gir", AnimalGender.FEMALE, AnimalStatus.ACTIVE, LocalDate.of(2022, 1, 1),
        "/api/v1/animals/" + animalId + "/photo", Instant.now(), Instant.now());
    when(animalService.updateAnimalPhoto("farmer@vetra.co.in", animalId, "/api/v1/animals/" + animalId + "/photo"))
        .thenReturn(response);

    ApiResponse<AnimalResponse> result = controller.uploadPhoto(principal, animalId, file);

    assertEquals(200, result.status());
    assertEquals("/api/v1/animals/" + animalId + "/photo", result.data().photoUrl());
    verify(mediaStorageService).storeEntityImage("animals", animalId.toString(), file);
  }

  @Test
  void testUploadPhotoAccessDeniedForNonOwner() {
    UUID animalId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    when(animalService.verifyAndGetAnimalForPhotoAccess("farmer@vetra.co.in", animalId))
        .thenThrow(new UnauthorizedResourceAccessException("Access denied: You do not own this animal record", "ANIMAL_002"));

    assertThrows(UnauthorizedResourceAccessException.class, () ->
        controller.uploadPhoto(principal, animalId, file));
  }

  @Test
  void testGetPhotoSuccess() {
    UUID animalId = UUID.randomUUID();
    Animal animal = new Animal();
    animal.setId(animalId);
    when(animalService.verifyAndGetAnimalForPhotoAccess("farmer@vetra.co.in", animalId)).thenReturn(animal);

    Resource resource = new ByteArrayResource(new byte[] {1, 2, 3});
    MediaResource mediaResource = new MediaResource(resource, MediaType.IMAGE_JPEG, animalId + ".jpg");
    when(mediaStorageService.loadEntityImage("animals", animalId.toString())).thenReturn(mediaResource);

    ResponseEntity<Resource> response = controller.getPhoto(principal, animalId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
  }

  @Test
  void testDeletePhotoSuccess() {
    UUID animalId = UUID.randomUUID();
    Animal animal = new Animal();
    animal.setId(animalId);
    when(animalService.verifyAndGetAnimalForPhotoAccess("farmer@vetra.co.in", animalId)).thenReturn(animal);

    AnimalResponse response = new AnimalResponse(
        animalId, UUID.randomUUID(), "Ramesh", "Gauri", "TAG-101", "QR-101",
        Species.CATTLE, "Gir", AnimalGender.FEMALE, AnimalStatus.ACTIVE, LocalDate.of(2022, 1, 1),
        null, Instant.now(), Instant.now());
    when(animalService.removeAnimalPhoto("farmer@vetra.co.in", animalId)).thenReturn(response);

    ApiResponse<AnimalResponse> result = controller.deletePhoto(principal, animalId);

    assertEquals(200, result.status());
    verify(mediaStorageService).deleteEntityImage("animals", animalId.toString());
  }
}
