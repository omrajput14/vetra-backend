package app.vetra.media.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.media.config.MediaStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class MediaStorageServiceTest {

  @TempDir
  Path tempDir;

  private MediaStorageService mediaStorageService;

  @BeforeEach
  void setUp() {
    MediaStorageProperties properties = new MediaStorageProperties();
    properties.setStorageDir(tempDir.toString());
    properties.setMaxFileSizeBytes(1024 * 1024); // 1MB for test
    mediaStorageService = new MediaStorageService(properties);
    mediaStorageService.init();
  }

  @Test
  void testStoreAndLoadValidJpeg() {
    byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes);

    String entityId = UUID.randomUUID().toString();
    String filename = mediaStorageService.storeEntityImage("animals", entityId, file);

    assertEquals(entityId + ".jpg", filename);

    MediaStorageService.MediaResource resource = mediaStorageService.loadEntityImage("animals", entityId);
    assertNotNull(resource);
    assertTrue(resource.resource().exists());
    assertEquals("image/jpeg", resource.mediaType().toString());
  }

  @Test
  void testStoreAndLoadValidPng() {
    byte[] pngBytes = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D
    };
    MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngBytes);

    String entityId = UUID.randomUUID().toString();
    String filename = mediaStorageService.storeEntityImage("profiles", entityId, file);

    assertEquals(entityId + ".png", filename);

    MediaStorageService.MediaResource resource = mediaStorageService.loadEntityImage("profiles", entityId);
    assertNotNull(resource);
    assertTrue(resource.resource().exists());
    assertEquals("image/png", resource.mediaType().toString());
  }

  @Test
  void testRejectCorruptOrNonImageFile() {
    byte[] textBytes = "This is definitely not an image file!".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", textBytes);

    String entityId = UUID.randomUUID().toString();
    assertThrows(BusinessRuleException.class, () ->
        mediaStorageService.storeEntityImage("animals", entityId, file));
  }

  @Test
  void testRejectEmptyFile() {
    MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
    String entityId = UUID.randomUUID().toString();
    assertThrows(BusinessRuleException.class, () ->
        mediaStorageService.storeEntityImage("animals", entityId, emptyFile));
  }

  @Test
  void testRejectFileExceedingMaxSize() {
    byte[] largeBytes = new byte[2 * 1024 * 1024]; // 2MB > 1MB limit
    largeBytes[0] = (byte) 0xFF;
    largeBytes[1] = (byte) 0xD8;
    largeBytes[2] = (byte) 0xFF;

    MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);
    String entityId = UUID.randomUUID().toString();
    assertThrows(BusinessRuleException.class, () ->
        mediaStorageService.storeEntityImage("animals", entityId, file));
  }

  @Test
  void testIdempotentReplacementDoesNotLeaveDuplicates() {
    String entityId = UUID.randomUUID().toString();

    // 1. Upload first as PNG
    byte[] pngBytes = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    MockMultipartFile file1 = new MockMultipartFile("file", "first.png", "image/png", pngBytes);
    mediaStorageService.storeEntityImage("animals", entityId, file1);

    Path pngPath = tempDir.resolve("animals").resolve(entityId + ".png");
    assertTrue(Files.exists(pngPath));

    // 2. Upload replacement as JPG for the same entityId
    byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    MockMultipartFile file2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", jpegBytes);
    mediaStorageService.storeEntityImage("animals", entityId, file2);

    Path jpgPath = tempDir.resolve("animals").resolve(entityId + ".jpg");
    assertTrue(Files.exists(jpgPath));
    // Old PNG must be deleted to avoid duplicate media on retries
    assertFalse(Files.exists(pngPath));
  }

  @Test
  void testDeleteEntityImage() {
    String entityId = UUID.randomUUID().toString();
    byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegBytes);
    mediaStorageService.storeEntityImage("animals", entityId, file);

    boolean deleted = mediaStorageService.deleteEntityImage("animals", entityId);
    assertTrue(deleted);
    assertThrows(ResourceNotFoundException.class, () ->
        mediaStorageService.loadEntityImage("animals", entityId));
  }
}
