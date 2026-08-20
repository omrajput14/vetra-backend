package app.vetra.ai.controller;

import app.vetra.ai.dto.advisor.AIAdvisorSessionResponse;
import app.vetra.ai.dto.advisor.AdvisorMessageRequest;
import app.vetra.ai.dto.advisor.CreateAdvisorSessionRequest;
import app.vetra.ai.service.AIAdvisorService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller providing endpoints for AI Veterinary Advisor conversational sessions,
 * message exchanges, and clinical assessment retrieval.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "AI Veterinary Advisor",
    description = "Endpoints for context-aware conversational veterinary screening and clinical guidance")
@SecurityRequirement(name = "bearerAuth")
public class AIAdvisorController {

  private final AIAdvisorService advisorService;

  /**
   * Constructs AIAdvisorController.
   *
   * @param advisorService AI advisor service
   */
  public AIAdvisorController(AIAdvisorService advisorService) {
    this.advisorService = advisorService;
  }

  /** Initializes a new AI Veterinary Advisor session for an animal. */
  @PostMapping("/animals/{animalId}/ai/advisor/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create AI Advisor Session",
      description = "Initializes an interactive, context-aware veterinary screening session for the specified animal.")
  public ApiResponse<AIAdvisorSessionResponse> createSession(
      Principal principal,
      @PathVariable UUID animalId,
      @Valid @RequestBody(required = false) CreateAdvisorSessionRequest request) {
    AIAdvisorSessionResponse response =
        advisorService.createSession(principal.getName(), animalId, request);
    return ApiResponse.created("AI Veterinary Advisor session initialized", response);
  }

  /** Sends an owner message to an active advisor session. */
  @PostMapping("/ai/advisor/sessions/{sessionId}/messages")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Send Message to AI Advisor",
      description = "Submits an owner observation or answer to the active advisor session and returns the AI response.")
  public ApiResponse<AIAdvisorSessionResponse> sendMessage(
      Principal principal,
      @PathVariable UUID sessionId,
      @Valid @RequestBody AdvisorMessageRequest request) {
    AIAdvisorSessionResponse response =
        advisorService.sendMessage(principal.getName(), sessionId, request);
    return ApiResponse.ok("AI Advisor message processed successfully", response);
  }

  /** Retrieves full session details and clinical assessment by session ID. */
  @GetMapping("/ai/advisor/sessions/{sessionId}")
  @Operation(
      summary = "Get AI Advisor Session",
      description = "Retrieves an existing AI advisor session by ID including full conversation turns and structured assessment.")
  public ApiResponse<AIAdvisorSessionResponse> getSession(
      Principal principal, @PathVariable UUID sessionId) {
    AIAdvisorSessionResponse response = advisorService.getSession(principal.getName(), sessionId);
    return ApiResponse.ok("AI Advisor session retrieved successfully", response);
  }

  /** Lists past AI advisor sessions for a specific animal. */
  @GetMapping("/animals/{animalId}/ai/advisor/sessions")
  @Operation(
      summary = "List AI Advisor Sessions for Animal",
      description = "Returns paginated AI Veterinary Advisor sessions for a registered animal.")
  public ApiResponse<Page<AIAdvisorSessionResponse>> listSessionsForAnimal(
      Principal principal,
      @PathVariable UUID animalId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AIAdvisorSessionResponse> page =
        advisorService.listSessionsForAnimal(principal.getName(), animalId, pageable);
    return ApiResponse.ok("AI Advisor sessions retrieved successfully", page);
  }
}
