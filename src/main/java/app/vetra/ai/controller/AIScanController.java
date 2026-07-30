package app.vetra.ai.controller;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.dto.VerifyAIScanRequest;
import app.vetra.ai.service.AIScanService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing AI Diagnostic Scan infrastructure endpoints.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Diagnostic Support Module", description = "Endpoints for creating, listing, viewing, and verifying AI diagnostic livestock scans")
public class AIScanController {

  private final AIScanService aiScanService;

  /** Constructor injection. */
  public AIScanController(AIScanService aiScanService) {
    this.aiScanService = aiScanService;
  }

  /** Creates a new AI diagnostic scan request for an animal image. */
  @PostMapping("/scans")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create AI Diagnostic Scan", description = "Submits a livestock image for AI diagnostic processing")
  public ApiResponse<AIScanResponse> createScan(
      Principal principal, @Valid @RequestBody CreateAIScanRequest request) {
    AIScanResponse response = aiScanService.createScan(principal.getName(), request);
    return ApiResponse.created("AI diagnostic scan registered successfully", response);
  }

  /** Retrieves an AI scan by ID. */
  @GetMapping("/scans/{id}")
  @Operation(summary = "Get AI Scan Details", description = "Fetches complete AI scan details by UUID")
  public ApiResponse<AIScanResponse> getScanById(Principal principal, @PathVariable("id") UUID id) {
    AIScanResponse response = aiScanService.getScanById(principal.getName(), id);
    return ApiResponse.ok("AI diagnostic scan details retrieved successfully", response);
  }

  /** Retrieves list of AI scans for active user (non-paginated). */
  @GetMapping("/scans")
  @Operation(summary = "List AI Scans", description = "Lists AI diagnostic scans for the authenticated user")
  public ApiResponse<List<AIScanResponse>> listScans(Principal principal) {
    List<AIScanResponse> response = aiScanService.listScans(principal.getName());
    return ApiResponse.ok("AI diagnostic scans retrieved successfully", response);
  }

  /** Retrieves paginated AI scans for active user. */
  @GetMapping("/scans/page")
  @Operation(summary = "Paginated List of AI Scans", description = "Lists paginated AI diagnostic scans with page, size, and sort parameters")
  public ApiResponse<Page<AIScanResponse>> listScansPaginated(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<AIScanResponse> response = aiScanService.listScans(principal.getName(), pageable);
    return ApiResponse.ok("Paginated AI diagnostic scans retrieved successfully", response);
  }

  /** Veterinarian verification endpoint to accept or correct an AI scan result. */
  @PatchMapping("/scans/{id}/verify")
  @Operation(summary = "Verify AI Scan Result", description = "Allows a licensed veterinarian to verify, accept, or correct an AI diagnostic result")
  public ApiResponse<AIScanResponse> verifyScan(
      Principal principal,
      @PathVariable("id") UUID id,
      @Valid @RequestBody VerifyAIScanRequest request) {
    AIScanResponse response = aiScanService.verifyScan(principal.getName(), id, request);
    return ApiResponse.ok("AI diagnostic scan verification submitted successfully", response);
  }
}
