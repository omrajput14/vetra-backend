package app.vetra.disease.controller;

import app.vetra.disease.geo.AdministrativeBoundaryService;
import app.vetra.disease.geo.GeoJsonFeatureCollection;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing RFC 7946 compliant administrative boundary GeoJSON features
 * and dynamic district metadata.
 */
@RestController
@RequestMapping("/api/v1/geo")
@Tag(name = "GIS & Administrative Boundaries", description = "Endpoints for geographic boundary layers and spatial extents")
@SecurityRequirement(name = "bearerAuth")
public class AdministrativeBoundaryController {

  private final AdministrativeBoundaryService boundaryService;

  /**
   * Constructor injection.
   *
   * @param boundaryService boundary management service
   */
  public AdministrativeBoundaryController(AdministrativeBoundaryService boundaryService) {
    this.boundaryService = boundaryService;
  }

  /**
   * Retrieves administrative boundaries for Maharashtra (State, District, Taluka).
   *
   * @param level administrative level filter (ALL, STATE, DISTRICT, TALUKA)
   * @param district optional district filter
   * @param state state filter (defaults to Maharashtra)
   * @return {@link ApiResponse} wrapping {@link GeoJsonFeatureCollection}
   */
  @GetMapping("/boundaries")
  @Operation(
      summary = "Get Administrative Boundaries GeoJSON",
      description = "Retrieves RFC 7946 compliant administrative boundary GeoJSON FeatureCollection for Maharashtra.")
  public ApiResponse<GeoJsonFeatureCollection> getBoundaries(
      @Parameter(description = "Administrative level (ALL, STATE, DISTRICT, TALUKA)")
      @RequestParam(value = "level", required = false, defaultValue = "ALL")
      String level,
      @Parameter(description = "Optional district name filter (e.g. Pune, Satara)")
      @RequestParam(value = "district", required = false)
      String district,
      @Parameter(description = "State name filter (default: Maharashtra)")
      @RequestParam(value = "state", required = false, defaultValue = "Maharashtra")
      String state) {
    GeoJsonFeatureCollection featureCollection = boundaryService.getBoundaries(level, district, state);
    return ApiResponse.ok("Administrative boundaries retrieved successfully", featureCollection);
  }

  /**
   * Retrieves list of available Maharashtra districts extracted dynamically from the dataset.
   *
   * @return {@link ApiResponse} wrapping list of district names
   */
  @GetMapping("/districts")
  @Operation(
      summary = "Get Available Districts",
      description = "Retrieves unique district names sourced dynamically from the administrative boundary dataset.")
  public ApiResponse<List<String>> getAvailableDistricts() {
    List<String> districts = boundaryService.getAvailableDistricts();
    return ApiResponse.ok("Administrative districts retrieved successfully", districts);
  }
}
