package app.vetra.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for secure, isolated media storage.
 */
@Configuration
@ConfigurationProperties(prefix = "vetra.media")
public class MediaStorageProperties {

  /** Base filesystem directory for storing uploaded media files. */
  private String storageDir = "./media-uploads";

  /** Maximum allowed upload size in bytes (default 10 MB). */
  private long maxFileSizeBytes = 10 * 1024 * 1024;

  public String getStorageDir() {
    return storageDir;
  }

  public void setStorageDir(String storageDir) {
    this.storageDir = storageDir;
  }

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    this.maxFileSizeBytes = maxFileSizeBytes;
  }
}
