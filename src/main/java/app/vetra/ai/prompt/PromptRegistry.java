package app.vetra.ai.prompt;

import app.vetra.ai.exception.AIConfigurationException;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Centralized registry for managing AI prompt descriptors. */
@Component
public class PromptRegistry {

  private static final Logger log = LoggerFactory.getLogger(PromptRegistry.class);

  private final Map<String, PromptDescriptor> prompts = new ConcurrentHashMap<>();
  private final PromptTemplateLoader loader;

  /**
   * Constructs the registry with the given loader.
   *
   * @param loader the prompt template loader
   */
  public PromptRegistry(PromptTemplateLoader loader) {
    this.loader = loader;
  }

  /** Initializes the registry by discovering and loading all prompts on classpath. */
  @PostConstruct
  public void initialize() {
    List<PromptDescriptor> loadedPrompts = loader.loadPrompts();
    for (PromptDescriptor descriptor : loadedPrompts) {
      if (prompts.containsKey(descriptor.promptId())) {
        throw new IllegalStateException("Duplicate prompt ID detected: " + descriptor.promptId());
      }
      prompts.put(descriptor.promptId(), descriptor);
    }
    log.info("PromptRegistry initialized with {} prompt(s).", prompts.size());
  }

  /**
   * Retrieves a prompt descriptor by ID.
   *
   * @param promptId the unique identifier of the prompt
   * @return the requested PromptDescriptor
   * @throws AIConfigurationException if the prompt is not found or is disabled
   */
  public PromptDescriptor getPrompt(String promptId) {
    PromptDescriptor descriptor = prompts.get(promptId);
    if (descriptor == null) {
      throw new AIConfigurationException(
          "Prompt not found in registry: " + promptId, "AI_PROMPT_NOT_FOUND");
    }
    if (!descriptor.enabled()) {
      throw new AIConfigurationException(
          "Prompt is currently disabled: " + promptId, "AI_PROMPT_DISABLED");
    }
    return descriptor;
  }

  /**
   * Returns all loaded prompts.
   *
   * @return unmodifiable map of prompts
   */
  public Map<String, PromptDescriptor> getAllPrompts() {
    return Collections.unmodifiableMap(prompts);
  }
}
