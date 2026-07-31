package app.vetra.disease.controller;

import app.vetra.disease.dto.OutbreakResponse;
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
 * REST controller providing endpoints for outbreak cluster intelligence.
 */
@RestController
@RequestMapping("/api/v1/disease/outbreaks")
@Tag(name = "Disease Outbreak Intelligence", description = "Endpoints for viewing active and historical disease outbreak clusters")
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

  /** Retrieves outbreak cluster details by ID. */
  @GetMapping("/{id}")
  @Operation(summary = "Get Outbreak Details by ID", description = "Retrieves outbreak cluster details by UUID.")
  public ApiResponse<OutbreakResponse> getOutbreakById(@PathVariable("id") UUID id) {
    OutbreakResponse response = diseaseService.getOutbreak(id);
    return ApiResponse.ok("Outbreak cluster details retrieved successfully", response);
  }
}
