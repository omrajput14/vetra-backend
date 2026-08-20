package app.vetra.ai.provider;

import app.vetra.ai.model.AICapability;
import app.vetra.ai.model.AIRequest;
import app.vetra.ai.model.AIResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * No-operation AI provider. Used as the default provider during local development, CI environments,
 * and when no external AI provider is configured. Also serves as the last-resort fallback when all
 * real providers are unavailable.
 *
 * <p>This is a first-class provider — it is registered in the {@code ProviderRegistry} and routed
 * to explicitly via the {@code noop} provider name. It does not throw exceptions during routing; it
 * returns a safe, deterministic stub response.
 */
@Component("noOpAIProvider")
public class NoOpAIProvider implements AIProvider {

  private static final String PROVIDER_NAME = "noop";


  @Override
  public String providerName() {
    return PROVIDER_NAME;
  }

  /**
   * Always returns false. NoOp does not perform real health checks.
   *
   * @return false
   */
  @Override
  public boolean health() {
    return false;
  }

  /**
   * Returns true in local/CI environments (noop is always "available" for routing to succeed).
   * Health is false — the provider is reachable but does not perform real inference.
   *
   * @return true
   */
  @Override
  public boolean isAvailable() {
    return true;
  }

  /**
   * Returns a deterministic stub {@link AIResponse}. Used in CI pipelines and local development to
   * ensure the gateway routing path is fully exercised without external calls.
   *
   * @param request the incoming AI request
   * @param promptText the resolved prompt text (ignored)
   * @return a stub response with zero token usage
   */
  @Override
  public AIResponse execute(AIRequest request, String promptText) {
    if (request != null && request.promptId() != null && request.promptId().contains("advisor")) {
      String convHistory =
          request.variables() != null && request.variables().containsKey("conversationHistory")
              ? String.valueOf(request.variables().get("conversationHistory"))
              : "";

      String latestUserMessage =
          request.variables() != null && request.variables().containsKey("latestUserMessage")
              ? String.valueOf(request.variables().get("latestUserMessage"))
              : "";

      boolean isFollowUp = convHistory.contains("Advisor:");

      String advisorJson;
      if (!isFollowUp) {
        // Turn 1: QUESTIONING state with targeted clinical follow-up questions
        advisorJson =
            """
            {
              "conversationState": "QUESTIONING",
              "replyMessage": "I understand your concern. To help provide a better preliminary assessment, how long has your animal been showing these symptoms, and have you noticed any fever or discharge?",
              "followUpQuestions": [
                "Is she drinking water normally?",
                "Is there any nasal discharge or coughing?",
                "Has there been a recent change in feed or routine?"
              ]
            }
            """;
      } else {
        // Turn 2+: ASSESSMENT_GENERATED state with structured non-prescriptive guidance and factual consistency
        advisorJson = buildTurn2AdvisorJson(convHistory, latestUserMessage);
      }
      return new AIResponse(
          advisorJson, request.promptId(), PROVIDER_NAME, "noop-v1", 0, 0, "stop");
    }

    String stubJsonResponse =
        """
        {
          "possibleCondition": "Bovine Dermatophilosis (Suspected)",
          "confidence": 0.88,
          "severity": "MODERATE",
          "observations": [
            "Localized exudative crusts and matting of hair in a paintbrush pattern",
            "Superficial epidermal scaling along dorsal skin surface",
            "No deep ulceration or systemic necrotic tissue visible"
          ],
          "recommendedNextStep": "Isolate animal in dry shelter, protect from moisture, and arrange on-site clinical evaluation by a licensed veterinarian.",
          "requiresVeterinarianReview": true,
          "disclaimer": "This is an AI-assisted preliminary assessment and is not a confirmed veterinary diagnosis."
        }
        """;
    return new AIResponse(
        stubJsonResponse, request.promptId(), PROVIDER_NAME, "noop-v1", 0, 0, "stop");
  }

  private String buildTurn2AdvisorJson(String convHistory, String latestUserMessage) {
    String combined = (convHistory + " " + latestUserMessage).toLowerCase();

    List<String> userReports = new ArrayList<>();
    userReports.add("Reported reduced feed consumption");

    if (combined.contains("normally") || combined.contains("normal water") || combined.contains("drinking water")) {
      userReports.add("Reported normal water intake");
    }
    if (combined.contains("101.5") || combined.contains("normal temp") || combined.contains("no fever")) {
      userReports.add("Reported body temperature within normal range (101.5°F)");
    }
    if (combined.contains("no bloat") || combined.contains("without bloat")) {
      userReports.add("Confirmed absence of acute bloat");
    }
    if (combined.contains("no coughing") || combined.contains("no cough")) {
      userReports.add("Confirmed absence of coughing");
    }
    if (combined.contains("no diarrhea")) {
      userReports.add("Confirmed absence of diarrhea");
    }

    StringBuilder userReportsJson = new StringBuilder();
    for (int i = 0; i < userReports.size(); i++) {
      userReportsJson.append("\"").append(userReports.get(i)).append("\"");
      if (i < userReports.size() - 1) {
        userReportsJson.append(",\n      ");
      }
    }

    return """
        {
          "conversationState": "ASSESSMENT_GENERATED",
          "replyMessage": "Thank you for the additional details. Based on the reported symptoms and animal context, here is a preliminary assistive assessment.",
          "followUpQuestions": [],
          "assessment": {
            "possibleConditions": [
              {
                "condition": "Subacute Ruminal Acidosis / Early Bovine Respiratory Sign (Suspected)",
                "confidence": 0.82,
                "reasoning": "Reported reduction in feed intake is consistent with early digestive sluggishness, while preserved hydration and absence of fever reduce likelihood of acute systemic infection."
              }
            ],
            "userReportedSymptoms": [
              %s
            ],
            "keyObservations": [
              "Mild digestive sluggishness with preserved hydration",
              "Absence of acute abdominal tympany or systemic pyrexia",
              "Absence of acute recumbency or respiratory distress"
            ],
            "riskLevel": "MODERATE",
            "requiresVeterinarianReview": true,
            "recommendedNextStep": "Isolate animal in a clean dry shelter with access to fresh water and good quality dry roughage. Monitor rectal temperature twice daily and arrange an on-site clinical consultation with a licensed veterinarian.",
            "disclaimer": "This is an AI-assisted preliminary assessment and is not a confirmed veterinary diagnosis. Consult a licensed veterinarian for clinical diagnosis and treatment."
          }
        }
        """.formatted(userReportsJson.toString());
  }

  /**
   * NoOp supports VISION and JSON_MODE capabilities to allow gateway routing to succeed during
   * tests.
   *
   * @return capability set
   */
  @Override
  public Set<AICapability> supportedCapabilities() {
    return Set.of(AICapability.VISION, AICapability.JSON_MODE);
  }
}
