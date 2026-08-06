package app.vetra.ai.prompt;

import app.vetra.ai.exception.AIConfigurationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Enterprise template renderer for AI prompts. Strictly enforces that all variables declared in the
 * template (e.g. {{variableName}}) are provided in the context map.
 */
@Component
public class PromptRenderer {

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

  /**
   * Renders the template by replacing all {{variableName}} with values from the context.
   *
   * @param template the raw prompt template string
   * @param context variables provided by the caller
   * @return the fully rendered prompt string
   * @throws AIConfigurationException if any required variables are missing
   */
  public String render(String template, Map<String, Object> context) {
    if (template == null || template.isEmpty()) {
      return template;
    }

    Set<String> requiredVariables = extractVariables(template);
    Set<String> missingVariables = new HashSet<>();

    for (String varName : requiredVariables) {
      if (!context.containsKey(varName)) {
        missingVariables.add(varName);
      }
    }

    if (!missingVariables.isEmpty()) {
      throw new AIConfigurationException(
          "Missing required variables for prompt template: " + String.join(", ", missingVariables),
          "AI_PROMPT_MISSING_VARS");
    }

    // Deterministic replacement
    StringBuilder rendered = new StringBuilder();
    Matcher matcher = VARIABLE_PATTERN.matcher(template);
    while (matcher.find()) {
      String varName = matcher.group(1).trim();
      Object value = context.get(varName);
      String replacement = (value == null) ? "" : String.valueOf(value);
      // use Matcher.quoteReplacement to avoid issues with special characters in the replacement
      // string
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(rendered);

    return rendered.toString();
  }

  /** Extracts all unique variable names from the template. */
  private Set<String> extractVariables(String template) {
    Set<String> variables = new HashSet<>();
    Matcher matcher = VARIABLE_PATTERN.matcher(template);
    while (matcher.find()) {
      variables.add(matcher.group(1).trim());
    }
    return variables;
  }
}
