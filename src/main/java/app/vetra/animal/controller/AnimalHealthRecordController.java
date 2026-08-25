package app.vetra.animal.controller;

import app.vetra.animal.dto.AnimalHealthRecordDto;
import app.vetra.animal.dto.AnimalHealthStatusDto;
import app.vetra.animal.dto.CreateHealthRecordRequest;
import app.vetra.animal.service.AnimalHealthRecordService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST Controller exposing animal digital medical passport and health timeline endpoints. */
@RestController
@RequestMapping("/api/v1/animals/{animalId}/health-records")
@Tag(
    name = "Animal Medical Passport & Health Timeline Module",
    description = "Endpoints for viewing lifetime medical records, vaccinations, AI screenings, and adding clinical events")
public class AnimalHealthRecordController {

  private final AnimalHealthRecordService healthRecordService;

  public AnimalHealthRecordController(AnimalHealthRecordService healthRecordService) {
    this.healthRecordService = healthRecordService;
  }

  /** Retrieves full medical and health timeline for an animal (newest first). */
  @GetMapping
  @Operation(
      summary = "Get Animal Health Timeline",
      description = "Retrieves complete chronological medical history and timeline for an animal")
  public ApiResponse<List<AnimalHealthRecordDto>> getAnimalTimeline(
      Principal principal, @PathVariable("animalId") UUID animalId) {
    List<AnimalHealthRecordDto> timeline =
        healthRecordService.getAnimalTimeline(principal.getName(), animalId);
    return ApiResponse.ok("Animal health timeline retrieved successfully", timeline);
  }

  /** Appends a new clinical health record to the animal's lifetime medical passport. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create Health Timeline Record",
      description = "Appends a new medical, vaccination, or treatment record to the animal's passport")
  public ApiResponse<AnimalHealthRecordDto> createRecord(
      Principal principal,
      @PathVariable("animalId") UUID animalId,
      @Valid @RequestBody CreateHealthRecordRequest request) {
    AnimalHealthRecordDto record =
        healthRecordService.createRecord(principal.getName(), animalId, request);
    return ApiResponse.created("Health record added to passport timeline successfully", record);
  }

  /** Retrieves the latest dynamically computed health status for the animal. */
  @GetMapping("/latest-status")
  @Operation(
      summary = "Get Latest Health Status",
      description = "Retrieves current health status calculated from recent medical timeline events")
  public ApiResponse<AnimalHealthStatusDto> getLatestHealthStatus(
      Principal principal, @PathVariable("animalId") UUID animalId) {
    AnimalHealthStatusDto status =
        healthRecordService.getLatestHealthStatus(principal.getName(), animalId);
    return ApiResponse.ok("Animal health status retrieved successfully", status);
  }
}
