package app.vetra.media.service;

import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.media.config.MediaStorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for secure, isolated media file storage and retrieval.
 * Validates magic bytes, enforces size limits, prevents path traversal,
 * guarantees upload idempotency, and maintains storage path isolation.
 */
@Service
public class MediaStorageService {

  private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);

  private static final Pattern SAFE_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
  private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|webp)$");
  private static final List<String> SUPPORTED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

  // Magic byte signatures
  private static final byte[] JPEG_MAGIC = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_MAGIC = new byte[] {
      (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] RIFF_MAGIC = new byte[] {0x52, 0x49, 0x46, 0x46}; // WEBP starts with RIFF
  private static final byte[] WEBP_MAGIC = new byte[] {0x57, 0x45, 0x42, 0x50}; // WEBP at offset 8

  private final Path baseStoragePath;
  private final long maxFileSizeBytes;

  public MediaStorageService(MediaStorageProperties properties) {
    this.baseStoragePath = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
    this.maxFileSizeBytes = properties.getMaxFileSizeBytes();
  }

  @PostConstruct
  public void init() {
    try {
      Files.createDirectories(baseStoragePath);
      Files.createDirectories(baseStoragePath.resolve("animals"));
      Files.createDirectories(baseStoragePath.resolve("profiles"));
      log.info("MediaStorageService initialized at: {}", baseStoragePath);
    } catch (IOException e) {
      log.error("Failed to initialize media storage directories at {}", baseStoragePath, e);
      throw new IllegalStateException("Could not initialize media storage folder", e);
    }
  }

  /**
   * Record representing a loaded media resource alongside its detected MediaType.
   */
  public record MediaResource(Resource resource, MediaType mediaType, String filename) {}

  /**
   * Stores an image for a specific entity ID.
   * Idempotent: Deletes any pre-existing files for this entity ID before writing.
   *
   * @param subDirectory Subdirectory name ("animals" or "profiles")
   * @param entityId Unique entity ID (UUID or alphanumeric user identifier)
   * @param file The multipart file
   * @return The stored filename
   */
  public String storeEntityImage(String subDirectory, String entityId, MultipartFile file) {
    validateDirectoryAndIdentifier(subDirectory, entityId);

    if (file == null || file.isEmpty()) {
      throw new BusinessRuleException("Uploaded file cannot be empty", "MEDIA_003");
    }

    if (file.getSize() > maxFileSizeBytes) {
      throw new BusinessRuleException(
          "File size (" + file.getSize() + " bytes) exceeds maximum limit of " + maxFileSizeBytes + " bytes",
          "MEDIA_002");
    }

    String extension = detectImageExtension(file);
    String filename = entityId + "." + extension;

    Path targetDir = baseStoragePath.resolve(subDirectory).normalize();
    if (!targetDir.startsWith(baseStoragePath)) {
      throw new SecurityException("Storage directory traversal detected");
    }

    // Clean up any existing image files for this entity (idempotency enforcement)
    deleteEntityImage(subDirectory, entityId);

    try {
      Files.createDirectories(targetDir);
      Path destination = targetDir.resolve(filename).normalize();
      if (!destination.startsWith(targetDir)) {
        throw new SecurityException("Destination path traversal detected");
      }

      try (InputStream in = file.getInputStream()) {
        Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
      }

      log.info("Stored media: {}/{}", subDirectory, filename);
      return filename;
    } catch (IOException e) {
      log.error("Failed to store media file {} in {}", filename, subDirectory, e);
      throw new BusinessRuleException("Failed to store media file: " + e.getMessage(), "MEDIA_006");
    }
  }

  /**
   * Loads an entity image by searching for supported extensions.
   */
  public MediaResource loadEntityImage(String subDirectory, String entityId) {
    validateDirectoryAndIdentifier(subDirectory, entityId);

    Path targetDir = baseStoragePath.resolve(subDirectory).normalize();
    for (String ext : SUPPORTED_EXTENSIONS) {
      String candidateName = entityId + "." + ext;
      Path candidatePath = targetDir.resolve(candidateName).normalize();
      if (candidatePath.startsWith(targetDir) && Files.exists(candidatePath) && Files.isReadable(candidatePath)) {
        try {
          Resource resource = new UrlResource(candidatePath.toUri());
          MediaType mediaType = resolveMediaType(ext);
          return new MediaResource(resource, mediaType, candidateName);
        } catch (IOException e) {
          throw new ResourceNotFoundException("Error loading media file: " + candidateName, "MEDIA_007");
        }
      }
    }

    throw new ResourceNotFoundException("Media photo not found for " + subDirectory + "/" + entityId, "MEDIA_007");
  }

  /**
   * Deletes all image extensions for a given entity identifier.
   */
  public boolean deleteEntityImage(String subDirectory, String entityId) {
    validateDirectoryAndIdentifier(subDirectory, entityId);

    Path targetDir = baseStoragePath.resolve(subDirectory).normalize();
    boolean deleted = false;
    for (String ext : SUPPORTED_EXTENSIONS) {
      String candidateName = entityId + "." + ext;
      Path candidatePath = targetDir.resolve(candidateName).normalize();
      if (candidatePath.startsWith(targetDir) && Files.exists(candidatePath)) {
        try {
          Files.delete(candidatePath);
          deleted = true;
        } catch (IOException e) {
          log.warn("Could not delete previous media file: {}", candidatePath, e);
        }
      }
    }
    return deleted;
  }

  private void validateDirectoryAndIdentifier(String subDirectory, String entityId) {
    if (!"animals".equals(subDirectory) && !"profiles".equals(subDirectory)) {
      throw new BusinessRuleException("Invalid media directory", "MEDIA_004");
    }
    if (entityId == null || !SAFE_IDENTIFIER_PATTERN.matcher(entityId).matches()) {
      throw new BusinessRuleException("Invalid entity identifier format", "MEDIA_005");
    }
  }

  private String detectImageExtension(MultipartFile file) {
    byte[] header = new byte[12];
    try (InputStream in = file.getInputStream()) {
      int read = in.read(header);
      if (read < 12) {
        throw new BusinessRuleException("Corrupt or invalid image file header", "MEDIA_001");
      }
    } catch (IOException e) {
      throw new BusinessRuleException("Failed to read image content for validation", "MEDIA_001");
    }

    // Check JPEG: FF D8 FF
    if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
      return "jpg";
    }

    // Check PNG: 89 50 4E 47 0D 0A 1A 0A
    if (Arrays.equals(Arrays.copyOfRange(header, 0, 8), PNG_MAGIC)) {
      return "png";
    }

    // Check WEBP: "RIFF" at 0-3 and "WEBP" at 8-11
    if (Arrays.equals(Arrays.copyOfRange(header, 0, 4), RIFF_MAGIC)
        && Arrays.equals(Arrays.copyOfRange(header, 8, 12), WEBP_MAGIC)) {
      return "webp";
    }

    throw new BusinessRuleException(
        "Unsupported image format. Only JPEG, PNG, and WEBP files are permitted.",
        "MEDIA_001");
  }

  private MediaType resolveMediaType(String ext) {
    return switch (ext.toLowerCase()) {
      case "png" -> MediaType.IMAGE_PNG;
      case "webp" -> MediaType.parseMediaType("image/webp");
      case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
      default -> MediaType.APPLICATION_OCTET_STREAM;
    };
  }

  public Path getBaseStoragePath() {
    return baseStoragePath;
  }
}
