package app.vetra.ai.service;

import app.vetra.ai.dto.advisor.AIAdvisorAssessmentDTO;
import app.vetra.ai.dto.advisor.AIAdvisorMessageResponse;
import app.vetra.ai.dto.advisor.AIAdvisorSessionResponse;
import app.vetra.ai.dto.advisor.PossibleConditionDTO;
import app.vetra.ai.entity.AIAdvisorMessage;
import app.vetra.ai.entity.AIAdvisorRiskLevel;
import app.vetra.ai.entity.AIAdvisorSession;
import app.vetra.ai.entity.AIAdvisorSessionStatus;
import app.vetra.infrastructure.persistence.entity.Animal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Helper component for parsing AI advisor model outputs and mapping session entities to DTOs. */
@Component
public class AIAdvisorResponseMapper {

  private static final Logger log = LoggerFactory.getLogger(AIAdvisorResponseMapper.class);
  private final ObjectMapper objectMapper;

  /**
   * Constructs AIAdvisorResponseMapper.
   *
   * @param objectMapper JSON object mapper
   */
  public AIAdvisorResponseMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Parses the raw JSON response returned by the AI provider.
   *
   * @param rawJson JSON string
   * @param forceEmergency whether to enforce emergency status
   * @return parsed output record
   */
  public ParsedAdvisorOutput parseAdvisorResponse(String rawJson, boolean forceEmergency) {
    if (rawJson == null || rawJson.isBlank()) {
      return fallbackOutput(forceEmergency);
    }

    try {
      JsonNode root = objectMapper.readTree(rawJson);
      String rawState = root.path("conversationState").asText("QUESTIONING");
      String reply = root.path("replyMessage")
          .asText("I have noted your observations. Please consider scheduling a veterinary review.");

      List<String> questions = extractQuestions(root);
      AIAdvisorAssessmentDTO assessment = extractAssessment(root);

      AIAdvisorSessionStatus status = mapStatus(rawState, forceEmergency);
      AIAdvisorRiskLevel risk = resolveRisk(forceEmergency, assessment);

      return new ParsedAdvisorOutput(status, risk, true, reply, questions, assessment);
    } catch (Exception e) {
      log.warn("Error parsing AI advisor JSON response: {}. Falling back.", e.getMessage());
      return fallbackOutput(forceEmergency);
    }
  }

  private List<String> extractQuestions(JsonNode root) {
    List<String> list = new ArrayList<>();
    if (root.has("followUpQuestions") && root.get("followUpQuestions").isArray()) {
      for (JsonNode q : root.get("followUpQuestions")) {
        list.add(q.asText());
      }
    }
    return list;
  }

  private AIAdvisorAssessmentDTO extractAssessment(JsonNode root) {
    if (!root.has("assessment") || root.get("assessment").isNull()) {
      return null;
    }
    JsonNode aNode = root.get("assessment");
    List<PossibleConditionDTO> conditions = extractConditions(aNode);
    List<String> userReportedSymptoms = extractStringList(aNode, "userReportedSymptoms");
    List<String> observations = extractStringList(aNode, "keyObservations");

    AIAdvisorRiskLevel risk = AIAdvisorRiskLevel.fromString(aNode.path("riskLevel").asText("MODERATE"));
    boolean reqVet = aNode.path("requiresVeterinarianReview").asBoolean(true);
    String nextStep = aNode.path("recommendedNextStep")
        .asText("Keep animal sheltered, monitor temperature, and arrange on-site veterinary evaluation.");
    String disclaimer = aNode.path("disclaimer")
        .asText("This is an AI-assisted preliminary assessment and is not a confirmed veterinary diagnosis.");

    return new AIAdvisorAssessmentDTO(
        conditions, userReportedSymptoms, observations, risk, reqVet, nextStep, disclaimer);
  }

  private List<PossibleConditionDTO> extractConditions(JsonNode aNode) {
    List<PossibleConditionDTO> list = new ArrayList<>();
    if (aNode.has("possibleConditions") && aNode.get("possibleConditions").isArray()) {
      for (JsonNode c : aNode.get("possibleConditions")) {
        list.add(new PossibleConditionDTO(
            c.path("condition").asText("Preliminary Condition (Suspected)"),
            c.has("confidence") ? c.get("confidence").asDouble(0.70) : 0.70,
            c.path("reasoning").asText("Observed clinical symptoms.")));
      }
    }
    return list;
  }

  private List<String> extractStringList(JsonNode node, String fieldName) {
    List<String> list = new ArrayList<>();
    if (node.has(fieldName) && node.get(fieldName).isArray()) {
      for (JsonNode item : node.get(fieldName)) {
        String text = item.asText();
        if (text != null && !text.isBlank()) {
          list.add(text.trim());
        }
      }
    }
    return list;
  }

  private AIAdvisorSessionStatus mapStatus(String rawState, boolean forceEmergency) {
    if (forceEmergency) {
      return AIAdvisorSessionStatus.URGENT_VETERINARY_REVIEW;
    }
    if (rawState == null) {
      return AIAdvisorSessionStatus.QUESTIONING;
    }
    return switch (rawState.trim().toUpperCase()) {
      case "ASSESSMENT_GENERATED" -> AIAdvisorSessionStatus.ASSESSMENT_GENERATED;
      case "URGENT_VETERINARY_REVIEW" -> AIAdvisorSessionStatus.URGENT_VETERINARY_REVIEW;
      case "INSUFFICIENT_INFORMATION" -> AIAdvisorSessionStatus.INSUFFICIENT_INFORMATION;
      case "READY_FOR_ASSESSMENT" -> AIAdvisorSessionStatus.READY_FOR_ASSESSMENT;
      default -> AIAdvisorSessionStatus.QUESTIONING;
    };
  }

  private AIAdvisorRiskLevel resolveRisk(boolean forceEmergency, AIAdvisorAssessmentDTO assessment) {
    if (forceEmergency) {
      return AIAdvisorRiskLevel.CRITICAL;
    }
    return assessment != null ? assessment.riskLevel() : AIAdvisorRiskLevel.UNKNOWN;
  }

  private ParsedAdvisorOutput fallbackOutput(boolean forceEmergency) {
    AIAdvisorSessionStatus status =
        forceEmergency ? AIAdvisorSessionStatus.URGENT_VETERINARY_REVIEW : AIAdvisorSessionStatus.QUESTIONING;
    AIAdvisorRiskLevel risk = forceEmergency ? AIAdvisorRiskLevel.CRITICAL : AIAdvisorRiskLevel.MODERATE;
    String reply = forceEmergency
        ? "The reported symptoms indicate potentially urgent clinical concern. Please arrange immediate on-site veterinary attention without delay."
        : "I have recorded your animal's symptoms. How long has this condition been observed?";

    return new ParsedAdvisorOutput(status, risk, true, reply, List.of(), null);
  }

  /**
   * Maps an AIAdvisorSession entity to full AIAdvisorSessionResponse DTO.
   *
   * @param session session entity
   * @return session response DTO
   */
  public AIAdvisorSessionResponse mapToSessionResponse(AIAdvisorSession session) {
    AIAdvisorAssessmentDTO assessment = parseAssessmentJson(session.getAssessmentJson());
    List<AIAdvisorMessageResponse> messageResponses = mapMessages(session.getMessages());

    Animal animal = session.getAnimal();
    return new AIAdvisorSessionResponse(
        session.getId(),
        animal.getId(),
        animal.getAnimalName() != null ? animal.getAnimalName() : animal.getTagNumber(),
        animal.getSpecies() != null ? animal.getSpecies().name() : "Unknown",
        animal.getBreed(),
        session.getUser().getId(),
        session.getStatus(),
        session.getRiskLevel(),
        session.isRequiresVetReview(),
        session.getTurnCount(),
        assessment,
        messageResponses,
        session.getCreatedAt(),
        session.getUpdatedAt());
  }

  private AIAdvisorAssessmentDTO parseAssessmentJson(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, AIAdvisorAssessmentDTO.class);
    } catch (Exception e) {
      log.warn("Failed to deserialize session assessmentJson: {}", e.getMessage());
      return null;
    }
  }

  private List<AIAdvisorMessageResponse> mapMessages(List<AIAdvisorMessage> messages) {
    List<AIAdvisorMessageResponse> list = new ArrayList<>();
    if (messages == null) {
      return list;
    }
    for (AIAdvisorMessage msg : messages) {
      list.add(mapSingleMessage(msg));
    }
    return list;
  }

  private AIAdvisorMessageResponse mapSingleMessage(AIAdvisorMessage msg) {
    List<String> questions = new ArrayList<>();
    AIAdvisorAssessmentDTO msgAssessment = null;

    if (msg.getStructuredPayload() != null && !msg.getStructuredPayload().isBlank()) {
      try {
        JsonNode node = objectMapper.readTree(msg.getStructuredPayload());
        if (node.has("followUpQuestions") && node.get("followUpQuestions").isArray()) {
          for (JsonNode q : node.get("followUpQuestions")) {
            questions.add(q.asText());
          }
        }
        if (node.has("assessment") && !node.get("assessment").isNull()) {
          msgAssessment = objectMapper.treeToValue(node.get("assessment"), AIAdvisorAssessmentDTO.class);
        }
      } catch (Exception ignored) {
        // Optional payload
      }
    }

    return new AIAdvisorMessageResponse(
        msg.getId(),
        msg.getSenderType(),
        msg.getContent(),
        msg.getTurnNumber(),
        questions,
        msgAssessment,
        msg.getCreatedAt());
  }

  /** Parsed internal output model. */
  public record ParsedAdvisorOutput(
      AIAdvisorSessionStatus status,
      AIAdvisorRiskLevel riskLevel,
      boolean requiresVetReview,
      String replyMessage,
      List<String> followUpQuestions,
      AIAdvisorAssessmentDTO assessment) {}
}
