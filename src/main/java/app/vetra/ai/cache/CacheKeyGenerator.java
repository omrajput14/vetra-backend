package app.vetra.ai.cache;

import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.prompt.PromptDescriptor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Generates deterministic, SHA-256 cache keys for AI requests.
 *
 * <p>Ensures provider-independent and content-addressed cache key derivation from prompt metadata,
 * rendered template content, model parameters, and capabilities. Prompt text is never cached raw.
 */
@Component
public class CacheKeyGenerator {

  private static final String DEFAULT_PREFIX = "vetra:ai:cache:";

  /**
   * Generates a deterministic SHA-256 cache key.
   *
   * @param request AI request parameters
   * @param renderedPrompt rendered prompt text
   * @param descriptor prompt descriptor containing prompt ID and version
   * @return formatted deterministic cache key
   */
  public String generateKey(
      AIRequest request, String renderedPrompt, PromptDescriptor descriptor) {
    return generateKey(request, renderedPrompt, descriptor, DEFAULT_PREFIX);
  }

  /**
   * Generates a deterministic SHA-256 cache key with custom prefix.
   *
   * @param request AI request parameters
   * @param renderedPrompt rendered prompt text
   * @param descriptor prompt descriptor
   * @param keyPrefix cache key namespace prefix
   * @return formatted deterministic cache key
   */
  public String generateKey(
      AIRequest request,
      String renderedPrompt,
      PromptDescriptor descriptor,
      String keyPrefix) {

    String prefix = (keyPrefix != null && !keyPrefix.isBlank()) ? keyPrefix : DEFAULT_PREFIX;

    StringBuilder sb = new StringBuilder();
    sb.append("promptId=").append(descriptor.promptId()).append(";");
    sb.append("promptVersion=").append(descriptor.version()).append(";");
    sb.append("renderedHash=").append(hashString(renderedPrompt)).append(";");
    sb.append("provider=")
        .append(request.requestedProvider() != null ? request.requestedProvider().name() : "ANY")
        .append(";");

    Set<AICapability> capabilities = request.requiredCapabilities();
    if (capabilities != null && !capabilities.isEmpty()) {
      Set<String> sortedCaps = new TreeSet<>();
      for (AICapability cap : capabilities) {
        sortedCaps.add(cap.name());
      }
      sb.append("capabilities=").append(sortedCaps).append(";");
    } else {
      sb.append("capabilities=[];");
    }

    if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
      sb.append("imageHash=").append(hashString(request.imageUrl().trim())).append(";");
    }

    String compositeString = sb.toString();
    String sha256Hex = hashString(compositeString);

    return prefix + sha256Hex;
  }

  /**
   * Computes SHA-256 hex string for given input string.
   *
   * @param input raw input string
   * @return hex encoded SHA-256 string
   */
  public String hashString(String input) {
    if (input == null) {
      return "";
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm unavailable", e);
    }
  }
}
