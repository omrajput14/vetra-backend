package app.vetra.disease.controller;

import app.vetra.disease.dto.DiseaseReportResponse;
import app.vetra.disease.dto.OutbreakResponse;
import app.vetra.disease.dto.OutbreakStatisticsResponse;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.service.DiseaseService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing endpoints for intelligent outbreak cluster intelligence.
 */
@RestController
@RequestMapping("/api/v1/disease/outbreaks")
@Tag(name = "Disease Outbreak Intelligence", description = "Endpoints for viewing active, high-risk, and historical disease outbreak clusters")
@SecurityRequirement(name = "bearerAuth")
public class OutbreakController {

  private final DiseaseService diseaseService;

  /** Constructor injection. */
  public OutbreakController(DiseaseService diseaseService) {
    this.diseaseService = diseaseService;
  }

  /** Lists active or historical disease outbreaks. */
  @GetMapping
  @Operation(summary = "List Outbreak Clusters", description = "Retrieves active or historical disease outbreak clusters.")
  public ApiResponse<List<OutbreakResponse>> listOutbreaks(
      @RequestParam(value = "status", required = false) OutbreakStatus status) {
    List<OutbreakResponse> response = diseaseService.listOutbreaks(status);
    return ApiResponse.ok("Outbreak clusters retrieved successfully", response);
  }

  /** Retrieves high-risk outbreak clusters (HIGH or CRITICAL risk scores). */
  @GetMapping("/high-risk")
  @Operation(summary = "Get High Risk Outbreaks", description = "Retrieves active outbreak clusters with HIGH or CRITICAL risk severity scores.")
  public ApiResponse<List<OutbreakResponse>> getHighRiskOutbreaks() {
    List<OutbreakResponse> response = diseaseService.getHighRiskOutbreaks();
    return ApiResponse.ok("High-risk outbreak clusters retrieved successfully", response);
  }

  /** Generates epidemiological outbreak cluster summary statistics. */
  @GetMapping("/statistics")
  @Operation(summary = "Get Outbreak Statistics", description = "Retrieves summary epidemiological statistics for active and historical outbreaks.")
  public ApiResponse<OutbreakStatisticsResponse> getOutbreakStatistics() {
    OutbreakStatisticsResponse response = diseaseService.getOutbreakStatistics();
    return ApiResponse.ok("Outbreak statistics retrieved successfully", response);
  }

  /** Retrieves outbreak cluster details by ID. */
  @GetMapping("/{id}")
  @Operation(summary = "Get Outbreak Details by ID", description = "Retrieves outbreak cluster details by UUID.")
  public ApiResponse<OutbreakResponse> getOutbreakById(@PathVariable("id") UUID id) {
    OutbreakResponse response = diseaseService.getOutbreak(id);
    return ApiResponse.ok("Outbreak cluster details retrieved successfully", response);
  }

  /** Retrieves all disease reports contributing to a specific outbreak cluster. */
  @GetMapping("/{id}/reports")
  @Operation(summary = "Get Reports for Outbreak Cluster", description = "Retrieves all disease reports contributing to an outbreak cluster.")
  public ApiResponse<List<DiseaseReportResponse>> getReportsForOutbreak(@PathVariable("id") UUID id) {
    List<DiseaseReportResponse> response = diseaseService.getReportsForOutbreak(id);
    return ApiResponse.ok("Outbreak cluster reports retrieved successfully", response);
  }
}
