package app.vetra.ai.provider.gemini;

import org.springframework.stereotype.Component;

/**
 * Prompt engineering builder constructing structured instructions for Gemini Vision API.
 */
@Component
public class GeminiPromptBuilder {

  private static final String VETERINARY_SYSTEM_PROMPT = """
      You are a senior veterinary clinician specializing in livestock animal health and disease diagnostic support.
      Analyze the provided livestock diagnostic image carefully and identify visible clinical abnormalities, physical lesions, symptoms, or disease signs.

      Strictly return your response as a valid JSON object matching this exact JSON schema:
      {
        "condition": "Name of suspected clinical condition or 'Healthy'",
        "confidence": 0.85,
        "observations": ["List of physical symptoms or visual observations"],
        "recommendations": ["List of immediate care or containment steps"],
        "requiresVeterinarianReview": true
      }

      Rules:
      1. Output MUST be strictly valid raw JSON without any markdown formatting or code blocks.
      2. The confidence value MUST be a floating point number between 0.00 and 1.00.
      3. Always set requiresVeterinarianReview to true whenever any abnormality is detected.
      """;

  /**
   * Generates the system prompt instructing Gemini Vision to return structured JSON.
   *
   * @return structured system prompt string
   */
  public String buildPrompt() {
    return VETERINARY_SYSTEM_PROMPT;
  }
}
