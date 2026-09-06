package app.vetra.auth.controller;

import app.vetra.auth.dto.UserProfileDto;
import app.vetra.auth.service.UserProfilePhotoService;
import app.vetra.infrastructure.persistence.entity.User;
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
 * Controller for secure user profile photo upload, retrieval, and deletion.
 * Only the authenticated user can upload or remove their own profile photo.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile Photo Module", description = "Authorized media endpoints for user profile photos")
public class UserProfilePhotoController {

  private final UserProfilePhotoService profilePhotoService;
  private final MediaStorageService mediaStorageService;

  public UserProfilePhotoController(
      UserProfilePhotoService profilePhotoService, MediaStorageService mediaStorageService) {
    this.profilePhotoService = profilePhotoService;
    this.mediaStorageService = mediaStorageService;
  }

  /**
   * Uploads or updates the authenticated user's profile photo.
   */
  @PostMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Upload Profile Photo",
      description = "Uploads profile photo for the active authenticated user")
  public ApiResponse<UserProfileDto> uploadProfilePhoto(
      Principal principal,
      @RequestParam("file") MultipartFile file) {

    User user = profilePhotoService.getUserByIdentifier(principal.getName());
    mediaStorageService.storeEntityImage("profiles", user.getId().toString(), file);

    String photoEndpoint = "/api/v1/users/profile/photo";
    UserProfileDto updated = profilePhotoService.updateUserProfilePhoto(principal.getName(), photoEndpoint);

    return ApiResponse.ok("Profile photo uploaded successfully", updated);
  }

  /**
   * Retrieves the authenticated user's profile photo.
   * Requires authentication. Not globally public.
   */
  @GetMapping("/profile/photo")
  @Operation(
      summary = "Get Own Profile Photo",
      description = "Retrieves profile photo for active user")
  public ResponseEntity<Resource> getOwnProfilePhoto(Principal principal) {
    User user = profilePhotoService.getUserByIdentifier(principal.getName());
    MediaResource mediaResource = mediaStorageService.loadEntityImage("profiles", user.getId().toString());

    return ResponseEntity.ok()
        .contentType(mediaResource.mediaType())
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
        .body(mediaResource.resource());
  }

  /**
   * Retrieves another user's profile photo by userId (authenticated access only).
   */
  @GetMapping("/{userId}/profile-photo")
  @Operation(
      summary = "Get User Profile Photo by ID",
      description = "Retrieves profile photo of a specific user for authorized app views")
  public ResponseEntity<Resource> getUserProfilePhoto(
      Principal principal,
      @PathVariable UUID userId) {

    MediaResource mediaResource = mediaStorageService.loadEntityImage("profiles", userId.toString());

    return ResponseEntity.ok()
        .contentType(mediaResource.mediaType())
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
        .body(mediaResource.resource());
  }

  /**
   * Removes the authenticated user's profile photo.
   */
  @DeleteMapping("/profile/photo")
  @Operation(
      summary = "Delete Profile Photo",
      description = "Deletes profile photo for the active user")
  public ApiResponse<UserProfileDto> deleteProfilePhoto(Principal principal) {
    User user = profilePhotoService.getUserByIdentifier(principal.getName());
    mediaStorageService.deleteEntityImage("profiles", user.getId().toString());
    UserProfileDto updated = profilePhotoService.removeUserProfilePhoto(principal.getName());

    return ApiResponse.ok("Profile photo removed successfully", updated);
  }
}
