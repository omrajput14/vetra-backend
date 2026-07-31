package app.vetra.disease.controller;

import app.vetra.disease.dto.CreateDiseaseReportRequest;
import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.NearbyReportResponse;
import app.vetra.disease.service.DiseaseService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing disease surveillance reporting and spatial proximity queries.
 */
@RestController
@RequestMapping("/api/v1/disease/reports")
@Tag(name = "Disease Surveillance Module", description = "Endpoints for disease report submission, spatial search, and outbreak intelligence")
@SecurityRequirement(name = "bearerAuth")
public class DiseaseReportController {

  private final DiseaseService diseaseService;

  /** Constructor injection. */
  public DiseaseReportController(DiseaseService diseaseService) {
    this.diseaseService = diseaseService;
  }

  /** Submits a new disease report. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
  @Operation(summary = "Create Disease Report", description = "Submits a new verified or suspected disease report for an animal.")
  public ApiResponse<DiseaseReportResponse> createReport(
      Principal principal, @Valid @RequestBody CreateDiseaseReportRequest request) {
    DiseaseReportResponse response = diseaseService.createReport(principal.getName(), request);
    return ApiResponse.created("Disease report submitted successfully", response);
  }

  /** Retrieves a disease report by ID. */
  @GetMapping("/{id}")
  @Operation(summary = "Get Disease Report by ID", description = "Fetches a disease report by UUID.")
  public ApiResponse<DiseaseReportResponse> getReportById(
      Principal principal, @PathVariable("id") UUID id) {
    DiseaseReportResponse response = diseaseService.getReport(principal.getName(), id);
    return ApiResponse.ok("Disease report retrieved successfully", response);
  }

  /** Lists disease reports with pagination. */
  @GetMapping
  @Operation(summary = "List Disease Reports", description = "Retrieves paginated list of disease reports.")
  public ApiResponse<Page<DiseaseReportResponse>> listReports(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<DiseaseReportResponse> response = diseaseService.listReports(principal.getName(), pageable);
    return ApiResponse.ok("Disease reports retrieved successfully", response);
  }

  /** Searches nearby disease reports within geographic radius. */
  @GetMapping("/nearby")
  @Operation(summary = "Search Nearby Disease Reports", description = "Searches for disease reports within geographic radius in kilometers.")
  public ApiResponse<List<NearbyReportResponse>> searchNearbyReports(
      @RequestParam("latitude") Double latitude,
      @RequestParam("longitude") Double longitude,
      @RequestParam(value = "radiusKm", required = false, defaultValue = "25.0") Double radiusKm) {
    List<NearbyReportResponse> response = diseaseService.searchNearbyReports(latitude, longitude, radiusKm);
    return ApiResponse.ok("Nearby disease reports retrieved successfully", response);
  }
}
