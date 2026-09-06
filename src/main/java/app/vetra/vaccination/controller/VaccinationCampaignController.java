package app.vetra.vaccination.controller;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.infrastructure.idempotency.IdempotencyService;
import app.vetra.vaccination.dto.CreateVaccinationCampaignRequest;
import app.vetra.vaccination.dto.UpdateCampaignStatusRequest;
import app.vetra.vaccination.dto.VaccinationCampaignAuditLogResponse;
import app.vetra.vaccination.dto.VaccinationCampaignResponse;
import app.vetra.vaccination.dto.VaccinationCampaignStatisticsResponse;
import app.vetra.vaccination.enums.CampaignStatus;
import app.vetra.vaccination.service.VaccinationCampaignService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing official government vaccination campaigns and lifecycle states.
 */
@RestController
@RequestMapping("/api/v1/vaccination/campaigns")
@Tag(
    name = "Vaccination Campaigns",
    description = "Government vaccination campaign planning, execution, and ring vaccination APIs")
@SecurityRequirement(name = "bearerAuth")
public class VaccinationCampaignController {

  private final VaccinationCampaignService campaignService;
  private final IdempotencyService idempotencyService;

  /**
   * Constructor injection for campaign controller.
   */
  public VaccinationCampaignController(
      VaccinationCampaignService campaignService, IdempotencyService idempotencyService) {
    this.campaignService = campaignService;
    this.idempotencyService = idempotencyService;
  }

  /**
   * Launches a new vaccination campaign with idempotency guarantees.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Launch Vaccination Campaign",
      description = "Creates a planned vaccination campaign for a pathogen and geographic jurisdiction.")
  public ApiResponse<VaccinationCampaignResponse> createCampaign(
      Principal principal,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreateVaccinationCampaignRequest request) {

    return idempotencyService.executeOrGet(
        principal.getName(),
        idempotencyKey,
        "/api/v1/vaccination/campaigns",
        VaccinationCampaignResponse.class,
        () -> {
          VaccinationCampaignResponse response =
              campaignService.createCampaign(principal.getName(), request);
          return ApiResponse.created("Vaccination campaign launched successfully", response);
        });
  }

  /**
   * Retrieves paginated list of vaccination campaigns with optional filters.
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "List Vaccination Campaigns",
      description = "Retrieves paginated vaccination campaigns filtered by status or district.")
  public ApiResponse<Page<VaccinationCampaignResponse>> listCampaigns(
      @RequestParam(required = false) CampaignStatus status,
      @RequestParam(required = false) String district,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {

    Page<VaccinationCampaignResponse> page = campaignService.listCampaigns(status, district, pageable);
    return ApiResponse.ok("Vaccination campaigns retrieved successfully", page);
  }

  /**
   * Retrieves summary statistics for regional vaccination campaigns.
   */
  @GetMapping("/statistics")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Get Campaign Statistics",
      description = "Retrieves aggregate counts and dose execution metrics across all campaigns.")
  public ApiResponse<VaccinationCampaignStatisticsResponse> getStatistics() {
    VaccinationCampaignStatisticsResponse stats = campaignService.getCampaignStatistics();
    return ApiResponse.ok("Campaign statistics retrieved successfully", stats);
  }

  /**
   * Retrieves all currently active vaccination campaigns.
   */
  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "List Active Campaigns",
      description = "Retrieves all currently active vaccination campaigns.")
  public ApiResponse<List<VaccinationCampaignResponse>> getActiveCampaigns() {
    List<VaccinationCampaignResponse> active = campaignService.getActiveCampaigns();
    return ApiResponse.ok("Active campaigns retrieved successfully", active);
  }

  /**
   * Retrieves details of a specific vaccination campaign by ID.
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Get Campaign Details",
      description = "Retrieves complete metadata and dose execution status for a specific campaign.")
  public ApiResponse<VaccinationCampaignResponse> getCampaign(@PathVariable UUID id) {
    VaccinationCampaignResponse response = campaignService.getCampaignById(id);
    return ApiResponse.ok("Campaign retrieved successfully", response);
  }

  /**
   * Transitions the operational status of a vaccination campaign.
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Update Campaign Status",
      description = "Advances campaign lifecycle status (PLANNED -> ACTIVE -> COMPLETED, or CANCELLED).")
  public ApiResponse<VaccinationCampaignResponse> updateStatus(
      Principal principal,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCampaignStatusRequest request) {

    VaccinationCampaignResponse response =
        campaignService.updateCampaignStatus(principal.getName(), id, request);
    return ApiResponse.ok("Campaign status updated successfully", response);
  }

  /**
   * Retrieves the immutable audit log history for a specific campaign.
   */
  @GetMapping("/{id}/audit-logs")
  @PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
  @Operation(
      summary = "Get Campaign Audit Trail",
      description = "Retrieves chronological officer action and status transition log records.")
  public ApiResponse<List<VaccinationCampaignAuditLogResponse>> getAuditLogs(@PathVariable UUID id) {
    List<VaccinationCampaignAuditLogResponse> logs = campaignService.getCampaignAuditLogs(id);
    return ApiResponse.ok("Campaign audit logs retrieved successfully", logs);
  }
}
