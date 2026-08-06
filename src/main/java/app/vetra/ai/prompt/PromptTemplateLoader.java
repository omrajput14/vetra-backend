package app.vetra.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Loads prompt templates from the classpath during application startup. */
@Component
public class PromptTemplateLoader {

  private static final Logger log = LoggerFactory.getLogger(PromptTemplateLoader.class);
  private static final String PROMPTS_PATTERN = "classpath*:prompts/**/*.json";

  private final ObjectMapper objectMapper;
  private final ResourcePatternResolver resourceResolver;

  /**
   * Constructs the loader with the given object mapper.
   *
   * @param objectMapper the Jackson object mapper
   */
  public PromptTemplateLoader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.resourceResolver = new PathMatchingResourcePatternResolver();
  }

  /**
   * Discovers and parses all prompt JSON files in the classpath.
   *
   * @return list of loaded prompt descriptors
   */
  public List<PromptDescriptor> loadPrompts() {
    List<PromptDescriptor> descriptors = new ArrayList<>();
    try {
      Resource[] resources = resourceResolver.getResources(PROMPTS_PATTERN);
      for (Resource resource : resources) {
        if (resource.isReadable()) {
          try {
            PromptDescriptor descriptor =
                objectMapper.readValue(resource.getInputStream(), PromptDescriptor.class);
            descriptors.add(descriptor);
            log.info(
                "Loaded prompt descriptor: [{}] from {}",
                descriptor.promptId(),
                resource.getFilename());
          } catch (Exception ex) {
            log.error(
                "Failed to parse prompt template from {}: {}",
                resource.getFilename(),
                ex.getMessage());
            // Fail-fast on startup if a prompt is malformed
            throw new IllegalStateException(
                "Failed to parse prompt template: " + resource.getFilename(), ex);
          }
        }
      }
    } catch (IOException ex) {
      log.error("Failed to resolve prompt resources: {}", ex.getMessage());
      throw new IllegalStateException("Failed to resolve prompt resources", ex);
    }
    return descriptors;
  }
}
