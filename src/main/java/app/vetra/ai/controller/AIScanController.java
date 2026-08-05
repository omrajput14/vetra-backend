package app.vetra.ai.controller;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.ApproveAIScanRequest;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.dto.RejectAIScanRequest;
import app.vetra.ai.service.AIScanService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller providing endpoints for AI diagnostic scan creation, status queries, and
 * veterinarian approval/rejection workflows.
 */
@RestController
@RequestMapping("/api/v1/ai/scans")
@Tag(
    name = "AI Diagnostic Scans",
    description = "Endpoints for uploading, querying, approving, and rejecting AI diagnostic scans")
@SecurityRequirement(name = "bearerAuth")
public class AIScanController {

  private final AIScanService aiScanService;

  /** Constructor injection. */
  public AIScanController(AIScanService aiScanService) {
    this.aiScanService = aiScanService;
  }

  /** Registers a new AI diagnostic scan request for an animal. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Submit AI Diagnostic Scan",
      description = "Uploads a new diagnostic image scan for AI inference processing.")
  public ApiResponse<AIScanResponse> createScan(
      Principal principal, @Valid @RequestBody CreateAIScanRequest request) {
    AIScanResponse response = aiScanService.createScan(principal.getName(), request);
    return ApiResponse.created("AI diagnostic scan submitted successfully", response);
  }

  /** Fetches an AI diagnostic scan by ID. */
  @GetMapping("/{id}")
  @Operation(
      summary = "Get AI Scan by ID",
      description = "Retrieves diagnostic scan details and AI inference results.")
  public ApiResponse<AIScanResponse> getScanById(Principal principal, @PathVariable("id") UUID id) {
    AIScanResponse response = aiScanService.getScanById(principal.getName(), id);
    return ApiResponse.ok("AI scan details retrieved successfully", response);
  }

  /** Lists AI scans for the authenticated user (non-paginated). */
  @GetMapping
  @Operation(
      summary = "List AI Diagnostic Scans",
      description = "Retrieves AI diagnostic scans relevant to active user role.")
  public ApiResponse<List<AIScanResponse>> listScans(Principal principal) {
    List<AIScanResponse> response = aiScanService.listScans(principal.getName());
    return ApiResponse.ok("AI scans retrieved successfully", response);
  }

  /** Lists AI scans for the authenticated user with Pageable pagination. */
  @GetMapping("/page")
  @Operation(
      summary = "Paginated List of AI Diagnostic Scans",
      description = "Retrieves paginated list of AI diagnostic scans.")
  public ApiResponse<Page<AIScanResponse>> listScansPaginated(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AIScanResponse> response = aiScanService.listScans(principal.getName(), pageable);
    return ApiResponse.ok("Paginated AI scans retrieved successfully", response);
  }

  /**
   * Approves an AI diagnostic scan result and automatically generates an Electronic Veterinary
   * Medical Record.
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Approve AI Diagnostic Scan",
      description =
          "Licensed veterinarian approves AI scan output and creates an immutable MedicalRecord entry.")
  public ApiResponse<AIScanResponse> approveScan(
      Principal principal,
      @PathVariable("id") UUID id,
      @Valid @RequestBody(required = false) ApproveAIScanRequest request) {
    AIScanResponse response = aiScanService.approveScan(principal.getName(), id, request);
    return ApiResponse.ok("AI diagnostic scan approved and MedicalRecord created", response);
  }

  /** Rejects an AI diagnostic scan result. */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Reject AI Diagnostic Scan",
      description = "Licensed veterinarian rejects AI scan output and records rejection reason.")
  public ApiResponse<AIScanResponse> rejectScan(
      Principal principal,
      @PathVariable("id") UUID id,
      @Valid @RequestBody RejectAIScanRequest request) {
    AIScanResponse response = aiScanService.rejectScan(principal.getName(), id, request);
    return ApiResponse.ok("AI diagnostic scan rejected successfully", response);
  }
}
