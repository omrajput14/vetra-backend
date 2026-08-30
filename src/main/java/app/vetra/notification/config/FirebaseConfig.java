package app.vetra.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Firebase Admin SDK using secure credentials from environment variables,
 * credential file path, or defaults to a dry-run fallback if unconfigured.
 */
@Configuration
public class FirebaseConfig {

  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

  @Value("${vetra.firebase.enabled:true}")
  private boolean enabled;

  @Value("${vetra.firebase.project-id:vetra-1ebfa}")
  private String projectId;

  @Value("${vetra.firebase.credentials-path:}")
  private String credentialsPath;

  @Value("${vetra.firebase.client-email:}")
  private String clientEmail;

  @Value("${vetra.firebase.private-key:}")
  private String privateKey;

  private boolean initialized = false;

  /** Initializes the FirebaseApp instance. */
  @PostConstruct
  public void initializeFirebase() {
    if (!enabled) {
      log.info("Firebase is explicitly disabled via configuration.");
      return;
    }

    if (!FirebaseApp.getApps().isEmpty()) {
      log.info("FirebaseApp already initialized.");
      this.initialized = true;
      return;
    }

    try {
      GoogleCredentials credentials = resolveCredentials();
      if (credentials != null) {
        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        FirebaseApp.initializeApp(options);
        this.initialized = true;
        log.info("Firebase Admin SDK successfully initialized for project: {}", projectId);
      } else {
        log.warn(
            "No Firebase service account credentials found (env vars or file). Operating in mock/dry-run mode.");
      }
    } catch (Exception e) {
      log.error("Failed to initialize Firebase Admin SDK: {}. Falling back to dry-run mode.", e.getMessage());
    }
  }

  private GoogleCredentials resolveCredentials() {
    try {
      String resolvedPath = resolveCredentialPath();
      if (resolvedPath != null) {
        log.info("Loading Firebase Admin SDK credentials from path: {}", resolvedPath);
        try (InputStream serviceAccount = new FileInputStream(resolvedPath)) {
          return GoogleCredentials.fromStream(serviceAccount);
        }
      }

      if (clientEmail != null && !clientEmail.isBlank() && privateKey != null && !privateKey.isBlank()) {
        log.info("Loading Firebase credentials from environment variables for: {}", clientEmail);
        String formattedKey = privateKey.replace("\\n", "\n");
        String json =
            String.format(
                "{\n"
                    + "  \"type\": \"service_account\",\n"
                    + "  \"project_id\": \"%s\",\n"
                    + "  \"client_email\": \"%s\",\n"
                    + "  \"private_key\": \"%s\"\n"
                    + "}",
                projectId, clientEmail, formattedKey.replace("\"", "\\\""));

        try (InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
          return GoogleCredentials.fromStream(is);
        }
      }

      // Try Application Default Credentials (e.g. GOOGLE_APPLICATION_CREDENTIALS)
      return GoogleCredentials.getApplicationDefault();
    } catch (Exception e) {
      log.debug("Standard ADC lookup returned: {}", e.getMessage());
      return null;
    }
  }

  private String resolveCredentialPath() {
    String path = (credentialsPath != null && !credentialsPath.isBlank())
        ? credentialsPath
        : System.getenv("FIREBASE_CREDENTIALS_PATH");

    if (path == null || path.isBlank()) {
      return null;
    }

    if (path.startsWith("~")) {
      path = System.getProperty("user.home") + path.substring(1);
    }

    java.io.File file = new java.io.File(path);
    if (file.exists() && file.isFile()) {
      return file.getAbsolutePath();
    }

    log.warn("Specified Firebase credential path not found on disk: {}", path);
    return null;
  }

  public boolean isInitialized() {
    return initialized && !FirebaseApp.getApps().isEmpty();
  }
}
