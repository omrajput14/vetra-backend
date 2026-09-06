package app.vetra.animal.controller;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.service.AnimalService;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.media.service.MediaStorageService;
import app.vetra.media.service.MediaStorageService.MediaResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for secure, authorized animal photo management.
 * Enforces ownership verification so farmers can only upload/modify/view photos of animals they own.
 * Veterinarians and government officers may view animal photos according to surveillance/clinical rules.
 */
@RestController
@RequestMapping("/api/v1/animals/{id}/photo")
@Tag(name = "Animal Photo Module", description = "Authorized media endpoints for animal photos")
public class AnimalPhotoController {

  private final AnimalService animalService;
  private final MediaStorageService mediaStorageService;

  public AnimalPhotoController(
      AnimalService animalService, MediaStorageService mediaStorageService) {
    this.animalService = animalService;
    this.mediaStorageService = mediaStorageService;
  }

  /**
   * Uploads or updates an animal photo with strict farmer ownership verification.
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload Animal Photo",
      description = "Uploads a photo for an animal. Only the owning farmer can upload.")
  public ApiResponse<AnimalResponse> uploadPhoto(
      Principal principal,
      @PathVariable UUID id,
      @RequestParam("file") MultipartFile file) {

    // 1. Verify that user owns the animal first (will throw 403 if unauthorized)
    animalService.verifyAndGetAnimalForPhotoAccess(principal.getName(), id);

    // 2. Store image securely and idempotently
    mediaStorageService.storeEntityImage("animals", id.toString(), file);

    // 3. Update database record with the canonical server media endpoint
    String photoEndpoint = "/api/v1/animals/" + id + "/photo";
    AnimalResponse updated = animalService.updateAnimalPhoto(principal.getName(), id, photoEndpoint);

    return ApiResponse.ok("Animal photo uploaded successfully", updated);
  }

  /**
   * Streams the animal photo with server-side authorization checks.
   * Not globally public.
   */
  @GetMapping
  @Operation(
      summary = "Get Animal Photo",
      description = "Retrieves animal photo. Requires authentication and access authorization.")
  public ResponseEntity<Resource> getPhoto(
      Principal principal,
      @PathVariable UUID id) {

    // Verifies farmer ownership or vet/government access
    Animal animal = animalService.verifyAndGetAnimalForPhotoAccess(principal.getName(), id);

    MediaResource mediaResource = mediaStorageService.loadEntityImage("animals", id.toString());

    return ResponseEntity.ok()
        .contentType(mediaResource.mediaType())
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
        .body(mediaResource.resource());
  }

  /**
   * Deletes an animal photo with farmer ownership verification.
   */
  @DeleteMapping
  @Operation(
      summary = "Delete Animal Photo",
      description = "Deletes photo for an animal. Only the owning farmer can delete.")
  public ApiResponse<AnimalResponse> deletePhoto(
      Principal principal,
      @PathVariable UUID id) {

    animalService.verifyAndGetAnimalForPhotoAccess(principal.getName(), id);
    mediaStorageService.deleteEntityImage("animals", id.toString());
    AnimalResponse updated = animalService.removeAnimalPhoto(principal.getName(), id);

    return ApiResponse.ok("Animal photo deleted successfully", updated);
  }
}
