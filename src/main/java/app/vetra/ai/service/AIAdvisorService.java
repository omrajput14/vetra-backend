package app.vetra.ai.service;

import app.vetra.ai.agent.gateway.AgentGateway;
import app.vetra.ai.agent.model.AgentCapability;
import app.vetra.ai.agent.model.AgentRequest;
import app.vetra.ai.agent.model.AgentResponse;
import app.vetra.ai.dto.advisor.AIAdvisorSessionResponse;
import app.vetra.ai.dto.advisor.AdvisorMessageRequest;
import app.vetra.ai.dto.advisor.CreateAdvisorSessionRequest;
import app.vetra.ai.entity.AIAdvisorMessage;
import app.vetra.ai.entity.AIAdvisorRiskLevel;
import app.vetra.ai.entity.AIAdvisorSenderType;
import app.vetra.ai.entity.AIAdvisorSession;
import app.vetra.ai.entity.AIAdvisorSessionStatus;
import app.vetra.ai.repository.AIAdvisorMessageRepository;
import app.vetra.ai.repository.AIAdvisorSessionRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service managing interactive AI Veterinary Advisor conversational sessions,
 * context assembly, clinical screening, safety escalation, and assessment persistence.
 */
@Service
public class AIAdvisorService {

  private static final Logger log = LoggerFactory.getLogger(AIAdvisorService.class);
  private static final int MAX_CONVERSATION_TURNS = 15;

  private static final Set<String> EMERGENCY_KEYWORDS =
      Set.of(
          "cannot stand",
          "unable to stand",
          "downer",
          "severe bloat",
          "choking",
          "seizure",
          "convulsion",
          "profuse bleeding",
          "heavy bleeding",
          "gasping",
          "cyanosis",
          "collapsed",
          "unconscious");

  private final AIAdvisorSessionRepository sessionRepository;
  private final AIAdvisorMessageRepository messageRepository;
  private final AnimalRepository animalRepository;
  private final UserRepository userRepository;
  private final AIAdvisorContextBuilder contextBuilder;
  private final AIAdvisorResponseMapper responseMapper;
  private final AgentGateway agentGateway;
  private final ObjectMapper objectMapper;

  /**
   * Constructs AIAdvisorService with required collaborators.
   *
   * @param sessionRepository session repository
   * @param messageRepository message repository
   * @param animalRepository animal repository
   * @param userRepository user repository
   * @param contextBuilder context builder service
   * @param responseMapper response mapping service
   * @param agentGateway agent gateway
   * @param objectMapper JSON object mapper
   */
  public AIAdvisorService(
      AIAdvisorSessionRepository sessionRepository,
      AIAdvisorMessageRepository messageRepository,
      AnimalRepository animalRepository,
      UserRepository userRepository,
      AIAdvisorContextBuilder contextBuilder,
      AIAdvisorResponseMapper responseMapper,
      AgentGateway agentGateway,
      ObjectMapper objectMapper) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.animalRepository = animalRepository;
    this.userRepository = userRepository;
    this.contextBuilder = contextBuilder;
    this.responseMapper = responseMapper;
    this.agentGateway = agentGateway;
    this.objectMapper = objectMapper;
  }

  /**
   * Initializes a new AI Veterinary Advisor session for the specified animal.
   *
   * @param userIdentifier requesting user email or phone
   * @param animalId UUID of target animal
   * @param request optional initial message payload
   * @return initialized session response
   */
  @Transactional
  public AIAdvisorSessionResponse createSession(
      String userIdentifier, UUID animalId, CreateAdvisorSessionRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = getAnimalById(animalId);

    validateAnimalAccess(user, animal);

    AIAdvisorSession session =
        AIAdvisorSession.builder()
            .animal(animal)
            .user(user)
            .status(AIAdvisorSessionStatus.QUESTIONING)
            .riskLevel(AIAdvisorRiskLevel.UNKNOWN)
            .requiresVetReview(true)
            .turnCount(0)
            .build();

    session = sessionRepository.save(session);
    log.info(
        "Created AI Advisor session id={} for animalId={}, userId={}",
        session.getId(),
        animal.getId(),
        user.getId());

    if (request != null && request.initialMessage() != null && !request.initialMessage().isBlank()) {
      return executeMessageTurn(user, session, request.initialMessage().trim());
    }

    return responseMapper.mapToSessionResponse(session);
  }

  /**
   * Sends a user message to an active advisor session, executes clinical inference, and persists
   * the response turn.
   *
   * @param userIdentifier requesting user email or phone
   * @param sessionId UUID of active session
   * @param request message request payload
   * @return updated session response
   */
  @Transactional
  public AIAdvisorSessionResponse sendMessage(
      String userIdentifier, UUID sessionId, AdvisorMessageRequest request) {
    User user = getUserByEmailOrPhone(userIdentifier);
    AIAdvisorSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "AI Advisor session not found with ID: " + sessionId, "ADVISOR_001"));

    validateSessionAccess(user, session);

    if (session.getTurnCount() >= MAX_CONVERSATION_TURNS) {
      throw new BusinessRuleException(
          "Session has reached maximum conversation turns ("
              + MAX_CONVERSATION_TURNS
              + "). Please book a veterinary consultation for clinical evaluation.",
          "ADVISOR_002");
    }

    return executeMessageTurn(user, session, request.message().trim());
  }

  /**
   * Retrieves an advisor session by ID with access verification.
   *
   * @param userIdentifier requesting user email or phone
   * @param sessionId UUID of session
   * @return session response
   */
  @Transactional(readOnly = true)
  public AIAdvisorSessionResponse getSession(String userIdentifier, UUID sessionId) {
    User user = getUserByEmailOrPhone(userIdentifier);
    AIAdvisorSession session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "AI Advisor session not found with ID: " + sessionId, "ADVISOR_001"));

    validateSessionAccess(user, session);
    return responseMapper.mapToSessionResponse(session);
  }

  /**
   * Lists all advisor sessions for an animal with pagination and authorization verification.
   *
   * @param userIdentifier requesting user email or phone
   * @param animalId UUID of target animal
   * @param pageable pagination parameters
   * @return page of advisor session responses
   */
  @Transactional(readOnly = true)
  public Page<AIAdvisorSessionResponse> listSessionsForAnimal(
      String userIdentifier, UUID animalId, Pageable pageable) {
    User user = getUserByEmailOrPhone(userIdentifier);
    Animal animal = getAnimalById(animalId);

    validateAnimalAccess(user, animal);
    return sessionRepository
        .findByAnimalIdOrderByCreatedAtDesc(animalId, pageable)
        .map(responseMapper::mapToSessionResponse);
  }

  private AIAdvisorSessionResponse executeMessageTurn(
      User user, AIAdvisorSession session, String userMessageText) {
    int nextTurn = session.getTurnCount() + 1;

    // 1. Persist user message
    AIAdvisorMessage userMsg =
        AIAdvisorMessage.builder()
            .session(session)
            .senderType(AIAdvisorSenderType.USER)
            .content(userMessageText)
            .turnNumber(nextTurn)
            .build();
    session.addMessage(userMsg);
    messageRepository.save(userMsg);

    // 2. Check emergency red flag keywords for conservative escalation
    boolean emergencyDetected = hasEmergencyKeywords(userMessageText);

    // 3. Assemble sanitized context
    Animal animal = session.getAnimal();
    String animalContext = contextBuilder.buildAnimalContext(animal);
    String medicalHistory = contextBuilder.buildMedicalHistoryContext(animal);
    String previousScans = contextBuilder.buildPreviousScansContext(animal);
    String conversationHistory = contextBuilder.buildConversationHistoryContext(session.getMessages());

    Map<String, Object> inputVariables =
        Map.of(
            "animalContext", animalContext,
            "medicalHistory", medicalHistory,
            "previousScans", previousScans,
            "conversationHistory", conversationHistory,
            "latestUserMessage", userMessageText);

    AgentRequest agentRequest =
        new AgentRequest(
            AgentCapability.ADVISOR,
            inputVariables,
            null,
            true,
            null,
            Map.of());

    // 4. Execute AI Agent Gateway
    AgentResponse agentResponse = agentGateway.execute(agentRequest);

    // 5. Parse and apply structured output
    AIAdvisorResponseMapper.ParsedAdvisorOutput output =
        responseMapper.parseAdvisorResponse(agentResponse.rawResponse().content(), emergencyDetected);

    // 6. Update session state
    session.setTurnCount(nextTurn);
    session.setStatus(output.status());
    session.setRiskLevel(output.riskLevel());
    session.setRequiresVetReview(output.requiresVetReview());

    if (output.assessment() != null) {
      try {
        session.setAssessmentJson(objectMapper.writeValueAsString(output.assessment()));
      } catch (Exception e) {
        log.warn("Failed to serialize advisor assessment JSON: {}", e.getMessage());
      }
    }

    // 7. Persist AI Advisor reply message
    AIAdvisorMessage advisorMsg =
        AIAdvisorMessage.builder()
            .session(session)
            .senderType(AIAdvisorSenderType.ADVISOR)
            .content(output.replyMessage())
            .structuredPayload(agentResponse.rawResponse().content())
            .turnNumber(nextTurn)
            .build();
    session.addMessage(advisorMsg);
    messageRepository.save(advisorMsg);

    session = sessionRepository.save(session);
    return responseMapper.mapToSessionResponse(session);
  }

  private boolean hasEmergencyKeywords(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    String lower = text.toLowerCase();
    for (String keyword : EMERGENCY_KEYWORDS) {
      if (lower.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private void validateAnimalAccess(User user, Animal animal) {
    if (user.getRole() == UserRole.FARMER) {
      if (animal.getFarmer() == null
          || animal.getFarmer().getUser() == null
          || !animal.getFarmer().getUser().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException(
            "Farmers can only access AI advisor sessions for their registered livestock",
            "AUTH_007");
      }
    }
  }

  private void validateSessionAccess(User user, AIAdvisorSession session) {
    if (user.getRole() == UserRole.FARMER) {
      if (!session.getUser().getId().equals(user.getId())) {
        throw new UnauthorizedResourceAccessException(
            "Farmers can only access their own AI advisor sessions", "AUTH_008");
      }
    }
  }

  private Animal getAnimalById(UUID animalId) {
    return animalRepository
        .findById(animalId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Animal not found with ID: " + animalId, "ANIMAL_001"));
  }

  private User getUserByEmailOrPhone(String identifier) {
    return userRepository
        .findByEmail(identifier)
        .or(() -> userRepository.findByPhone(identifier))
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found: " + identifier, "USER_004"));
  }
}
