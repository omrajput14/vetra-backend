package app.vetra.mortality.controller;

import app.vetra.infrastructure.idempotency.IdempotencyService;
import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.mortality.dto.ConfirmMortalityRequest;
import app.vetra.mortality.dto.CreateMortalityReportRequest;
import app.vetra.mortality.dto.MortalityAuditResponse;
import app.vetra.mortality.dto.MortalityReportResponse;
import app.vetra.mortality.dto.RejectMortalityRequest;
import app.vetra.mortality.service.MortalityService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller managing animal mortality reporting, validation, and retrieval. */
@RestController
@RequestMapping("/api/v1/mortalities")
@Tag(
    name = "Animal Mortality Module",
    description = "Endpoints for livestock mortality reporting, idempotency, vet validation, and audit history")
@SecurityRequirement(name = "bearerAuth")
public class MortalityController {

  private final MortalityService mortalityService;
  private final IdempotencyService idempotencyService;

  /** Constructor injection. */
  public MortalityController(
      MortalityService mortalityService, IdempotencyService idempotencyService) {
    this.mortalityService = mortalityService;
    this.idempotencyService = idempotencyService;
  }

  /** Submits an animal mortality report with offline idempotency guarantees. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('FARMER')")
  @Operation(
      summary = "Report Animal Death",
      description =
          "Submits a farmer-reported livestock mortality event with idempotent retry safety.")
  public ApiResponse<MortalityReportResponse> reportMortality(
      Principal principal,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreateMortalityReportRequest request) {

    return idempotencyService.executeOrGet(
        principal.getName(),
        idempotencyKey,
        "/api/v1/mortalities",
        MortalityReportResponse.class,
        () -> {
          MortalityReportResponse response =
              mortalityService.createMortalityReport(principal.getName(), request);
          return ApiResponse.created("Animal death reported successfully", response);
        });
  }

  /** Retrieves pending mortality review cases assigned to the authenticated veterinarian. */
  @GetMapping("/pending")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "List Pending Mortality Cases for Veterinarian",
      description = "Retrieves paginated mortality reports assigned to the veterinarian for clinical review.")
  public ApiResponse<Page<MortalityReportResponse>> listPendingCases(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<MortalityReportResponse> response =
        mortalityService.listPendingCasesForVet(principal.getName(), pageable);
    return ApiResponse.ok("Pending mortality cases retrieved successfully", response);
  }

  /** Confirms an animal mortality report by a verified veterinarian. */
  @PostMapping("/{id}/confirm")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Confirm Animal Mortality Report",
      description = "Validates animal death, records clinical diagnosis, and logs audit trail.")
  public ApiResponse<MortalityReportResponse> confirmMortality(
      Principal principal,
      @PathVariable("id") UUID id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody ConfirmMortalityRequest request) {

    return idempotencyService.executeOrGet(
        principal.getName(),
        idempotencyKey,
        "/api/v1/mortalities/" + id + "/confirm",
        MortalityReportResponse.class,
        () -> {
          MortalityReportResponse response =
              mortalityService.confirmMortality(principal.getName(), id, request);
          return ApiResponse.ok("Mortality report confirmed successfully", response);
        });
  }

  /** Rejects an animal mortality report with clinical rationale. */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Reject Animal Mortality Report",
      description = "Rejects animal death report with documented clinical rationale and audit trail.")
  public ApiResponse<MortalityReportResponse> rejectMortality(
      Principal principal,
      @PathVariable("id") UUID id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody RejectMortalityRequest request) {

    return idempotencyService.executeOrGet(
        principal.getName(),
        idempotencyKey,
        "/api/v1/mortalities/" + id + "/reject",
        MortalityReportResponse.class,
        () -> {
          MortalityReportResponse response =
              mortalityService.rejectMortality(principal.getName(), id, request);
          return ApiResponse.ok("Mortality report rejected successfully", response);
        });
  }

  /** Retrieves the immutable audit log history for a mortality report. */
  @GetMapping("/{id}/audits")
  @PreAuthorize("hasAnyRole('FARMER', 'VETERINARIAN', 'ADMIN')")
  @Operation(
      summary = "Get Mortality Report Audit History",
      description = "Retrieves the verification and validation audit history for a mortality event.")
  public ApiResponse<List<MortalityAuditResponse>> getAudits(
      Principal principal, @PathVariable("id") UUID id) {
    List<MortalityAuditResponse> response =
        mortalityService.getAuditsForEvent(principal.getName(), id);
    return ApiResponse.ok("Mortality audit history retrieved successfully", response);
  }

  /** Retrieves a mortality report by its ID. */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('FARMER', 'VETERINARIAN', 'ADMIN')")
  @Operation(
      summary = "Get Mortality Report by ID",
      description = "Retrieves mortality report details by its UUID.")
  public ApiResponse<MortalityReportResponse> getMortalityById(
      Principal principal, @PathVariable("id") UUID id) {
    MortalityReportResponse response =
        mortalityService.getMortalityById(principal.getName(), id);
    return ApiResponse.ok("Mortality report retrieved successfully", response);
  }

  /** Retrieves mortality information for a specific animal. */
  @GetMapping("/animal/{animalId}")
  @PreAuthorize("hasAnyRole('FARMER', 'VETERINARIAN', 'ADMIN')")
  @Operation(
      summary = "Get Mortality Report by Animal ID",
      description = "Retrieves mortality report for a specified animal.")
  public ApiResponse<MortalityReportResponse> getMortalityByAnimalId(
      Principal principal, @PathVariable("animalId") UUID animalId) {
    MortalityReportResponse response =
        mortalityService.getMortalityByAnimalId(principal.getName(), animalId);
    return ApiResponse.ok("Animal mortality report retrieved successfully", response);
  }

  /** Lists mortality reports for the authenticated farmer with pagination. */
  @GetMapping
  @PreAuthorize("hasAnyRole('FARMER', 'VETERINARIAN', 'ADMIN')")
  @Operation(
      summary = "List Farmer Mortality Reports",
      description = "Retrieves a paginated list of mortality reports submitted by the farmer.")
  public ApiResponse<Page<MortalityReportResponse>> listMortalities(
      Principal principal,
      @PageableDefault(size = 20, sort = "reportedAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<MortalityReportResponse> response =
        mortalityService.listFarmerMortalities(principal.getName(), pageable);
    return ApiResponse.ok("Mortality reports retrieved successfully", response);
  }
}
