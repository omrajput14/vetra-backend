package app.vetra.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.auth.dto.UserProfileDto;
import app.vetra.auth.service.UserProfilePhotoService;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.media.service.MediaStorageService;
import app.vetra.media.service.MediaStorageService.MediaResource;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class UserProfilePhotoControllerTest {

  private UserProfilePhotoService profilePhotoService;
  private MediaStorageService mediaStorageService;
  private UserProfilePhotoController controller;
  private Principal principal;

  @BeforeEach
  void setUp() {
    profilePhotoService = mock(UserProfilePhotoService.class);
    mediaStorageService = mock(MediaStorageService.class);
    controller = new UserProfilePhotoController(profilePhotoService, mediaStorageService);
    principal = () -> "farmer@vetra.co.in";
  }

  @Test
  void testUploadProfilePhotoSuccess() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    when(profilePhotoService.getUserByIdentifier("farmer@vetra.co.in")).thenReturn(user);

    MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

    UserProfileDto dto = new UserProfileDto(
        userId, "farmer@vetra.co.in", "+919876543210", UserRole.FARMER, true, "en",
        "Ramesh", "Farm", "Village", "Taluka", "District", "State", 18.5, 73.8, 5,
        null, null, null, null, null, null, null, null,
        "/api/v1/users/profile/photo", null, null, null);
    when(profilePhotoService.updateUserProfilePhoto("farmer@vetra.co.in", "/api/v1/users/profile/photo"))
        .thenReturn(dto);

    ApiResponse<UserProfileDto> result = controller.uploadProfilePhoto(principal, file);

    assertEquals(200, result.status());
    assertEquals("/api/v1/users/profile/photo", result.data().profilePhotoUrl());
    verify(mediaStorageService).storeEntityImage("profiles", userId.toString(), file);
  }

  @Test
  void testGetOwnProfilePhotoSuccess() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    when(profilePhotoService.getUserByIdentifier("farmer@vetra.co.in")).thenReturn(user);

    Resource resource = new ByteArrayResource(new byte[] {1, 2, 3});
    MediaResource mediaResource = new MediaResource(resource, MediaType.IMAGE_JPEG, userId + ".jpg");
    when(mediaStorageService.loadEntityImage("profiles", userId.toString())).thenReturn(mediaResource);

    ResponseEntity<Resource> response = controller.getOwnProfilePhoto(principal);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
  }

  @Test
  void testDeleteProfilePhotoSuccess() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    when(profilePhotoService.getUserByIdentifier("farmer@vetra.co.in")).thenReturn(user);

    UserProfileDto dto = new UserProfileDto(
        userId, "farmer@vetra.co.in", "+919876543210", UserRole.FARMER, true, "en",
        "Ramesh", "Farm", "Village", "Taluka", "District", "State", 18.5, 73.8, 5,
        null, null, null, null, null, null, null, null,
        null, null, null, null);
    when(profilePhotoService.removeUserProfilePhoto("farmer@vetra.co.in")).thenReturn(dto);

    ApiResponse<UserProfileDto> result = controller.deleteProfilePhoto(principal);

    assertEquals(200, result.status());
    verify(mediaStorageService).deleteEntityImage("profiles", userId.toString());
  }
}
