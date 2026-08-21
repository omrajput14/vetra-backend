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

      String language = extractLanguage(request);
      boolean isFollowUp = convHistory.contains("Advisor:") || convHistory.contains("पशुवैद्यक:");

      String advisorJson;
      if (!isFollowUp) {
        advisorJson = buildTurn1AdvisorJson(language);
      } else {
        advisorJson = buildTurn2AdvisorJson(convHistory, latestUserMessage, language);
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

  private String extractLanguage(AIRequest request) {
    if (request == null || request.variables() == null) {
      return "en";
    }
    if (request.variables().containsKey("preferredLanguage")) {
      String lang = String.valueOf(request.variables().get("preferredLanguage")).trim().toLowerCase();
      if ("mr".equals(lang) || "marathi".equals(lang)) {
        return "mr";
      }
      if ("hi".equals(lang) || "hindi".equals(lang)) {
        return "hi";
      }
    }
    if (request.variables().containsKey("languageInstruction")) {
      String instr = String.valueOf(request.variables().get("languageInstruction")).toLowerCase();
      if (instr.contains("marathi") || instr.contains("mr")) {
        return "mr";
      }
      if (instr.contains("hindi") || instr.contains("hi")) {
        return "hi";
      }
    }
    return "en";
  }

  private String buildTurn1AdvisorJson(String language) {
    if ("mr".equals(language)) {
      return """
          {
            "conversationState": "QUESTIONING",
            "replyMessage": "आपल्या जनावराच्या समस्येबद्दल मी समजतो. अधिक चांगल्या प्राथमिक निदानासाठी, ही लक्षणे किती दिवसांपासून दिसत आहेत आणि आपण ताप किंवा स्त्राव पाहिला आहे का?",
            "followUpQuestions": [
              "गाय पाणी व्यवस्थित पीत आहे का?",
              "नाकातून स्त्राव किंवा खोकला येत आहे का?",
              "आहारात किंवा दैनंदिन दिनचर्येत काही बदल झाला आहे का?"
            ]
          }
          """;
    } else if ("hi".equals(language)) {
      return """
          {
            "conversationState": "QUESTIONING",
            "replyMessage": "मैं आपकी चिंता समझता हूँ। बेहतर प्रारंभिक मूल्यांकन के लिए, आपका पशु कितने समय से यह लक्षण दिखा रहा है, और क्या आपने बुखार या कोई स्राव देखा है?",
            "followUpQuestions": [
              "क्या वह सामान्य रूप से पानी पी रही है?",
              "क्या नाक से स्राव या खांसी आ रही है?",
              "क्या चारे या दिनचर्या में कोई हालिया बदलाव हुआ है?"
            ]
          }
          """;
    } else {
      return """
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
    }
  }

  private String buildTurn2AdvisorJson(String convHistory, String latestUserMessage, String language) {
    if ("mr".equals(language)) {
      return buildTurn2MarathiJson();
    } else if ("hi".equals(language)) {
      return buildTurn2HindiJson();
    } else {
      return buildTurn2EnglishJson(convHistory, latestUserMessage);
    }
  }

  private String buildTurn2MarathiJson() {
    return """
        {
          "conversationState": "ASSESSMENT_GENERATED",
          "replyMessage": "अधिक माहिती दिल्याबद्दल धन्यवाद. नोंदवलेली लक्षणे आणि जनावराच्या माहितीच्या आधारे येथे प्राथमिक साहाय्यक मूल्यांकन दिले आहे.",
          "followUpQuestions": [],
          "assessment": {
            "possibleConditions": [
              {
                "condition": "Subacute Ruminal Acidosis / Early Bovine Respiratory Sign (Suspected)",
                "confidence": 0.82,
                "reasoning": "चारा न खाणे हे पचनाच्या सुरुवातीच्या समस्येशी सुसंगत आहे, तर पाणी पिणे सामान्य असणे आणि ताप नसणे यामुळे तीव्र संसर्गाची शक्यता कमी होते."
              }
            ],
            "userReportedSymptoms": [
              "कमी चारा खाल्ल्याची नोंद"
            ],
            "keyObservations": [
              "पाणी पिणे सामान्य असताना सौम्य पचन मंदावणे",
              "पोटफुगी किंवा तीव्र ताप नसणे",
              "खाली पडणे किंवा श्वास घेण्यास अडचण नसणे"
            ],
            "riskLevel": "MODERATE",
            "requiresVeterinarianReview": true,
            "recommendedNextStep": "जनावराला स्वच्छ, कोरड्या जागेत वेगळे ठेवा आणि ताजे पाणी व चांगल्या प्रतीचा कोरडा चारा द्या. दिवसातून दोनदा तापमान तपासा आणि परवानाधारक पशुवैद्यकाकडून तपासणी करून घ्या.",
            "disclaimer": "हे एक एआय-सहाय्यक प्राथमिक मूल्यांकन आहे आणि हे पुष्टी केलेले पशुवैद्यकीय निदान नाही. क्लिनिकल निदान आणि उपचारांसाठी परवानाधारक पशुवैद्यांचा सल्ला घ्या."
          }
        }
        """;
  }

  private String buildTurn2HindiJson() {
    return """
        {
          "conversationState": "ASSESSMENT_GENERATED",
          "replyMessage": "अतिरिक्त जानकारी देने के लिए धन्यवाद। बताए गए लक्षणों और पशु की जानकारी के आधार पर यह प्रारंभिक सहायक मूल्यांकन है।",
          "followUpQuestions": [],
          "assessment": {
            "possibleConditions": [
              {
                "condition": "Subacute Ruminal Acidosis / Early Bovine Respiratory Sign (Suspected)",
                "confidence": 0.82,
                "reasoning": "चारा कम खाना पाचन की प्रारंभिक सुस्ती के अनुरूप है, जबकि पानी का सेवन सामान्य होना और बुखार न होना गंभीर संक्रमण की संभावना को कम करता है।"
              }
            ],
            "userReportedSymptoms": [
              "चारा कम खाने की सूचना"
            ],
            "keyObservations": [
              "पानी का सेवन सामान्य होने के साथ हल्का पाचन धीमा होना",
              "पेट फूलना या तेज बुखार की अनुपस्थिति",
              "बैठने में असमर्थता या सांस लेने में तकलीफ की अनुपस्थिति"
            ],
            "riskLevel": "MODERATE",
            "requiresVeterinarianReview": true,
            "recommendedNextStep": "पशु को साफ, सूखे आश्रय में अलग रखें और ताजा पानी व अच्छी गुणवत्ता वाला सूखा चारा दें। दिन में दो बार तापमान की निगरानी करें और लाइसेंस प्राप्त पशुचिकित्सक से परामर्श लें।",
            "disclaimer": "यह एक एआई-सहायक प्रारंभिक मूल्यांकन है और यह कोई पुष्ट पशु चिकित्सा निदान नहीं है। नैदानिक निदान और उपचार के लिए किसी लाइसेंस प्राप्त पशुचिकित्सक से परामर्श लें।"
          }
        }
        """;
  }

  private String buildTurn2EnglishJson(String convHistory, String latestUserMessage) {
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
