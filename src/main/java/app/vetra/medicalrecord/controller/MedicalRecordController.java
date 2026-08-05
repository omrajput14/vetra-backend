package app.vetra.medicalrecord.controller;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.medicalrecord.dto.CreateMedicalRecordRequest;
import app.vetra.medicalrecord.dto.MedicalRecordResponse;
import app.vetra.medicalrecord.service.MedicalRecordService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing Electronic Veterinary Medical Record endpoints. Enforces strict
 * immutability—no PUT or DELETE endpoints exist.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(
    name = "Medical Records Module",
    description = "Electronic Veterinary Medical Record (EVMR) management")
public class MedicalRecordController {

  private final MedicalRecordService medicalRecordService;

  /** Constructor injection. */
  public MedicalRecordController(MedicalRecordService medicalRecordService) {
    this.medicalRecordService = medicalRecordService;
  }

  /** Creates a medical record for a completed appointment (Veterinarians only). */
  @PostMapping("/medical-records")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create Medical Record",
      description = "Creates a permanent medical record for a COMPLETED appointment")
  public ApiResponse<MedicalRecordResponse> createMedicalRecord(
      Principal principal, @Valid @RequestBody CreateMedicalRecordRequest request) {
    MedicalRecordResponse response =
        medicalRecordService.createMedicalRecord(principal.getName(), request);
    return ApiResponse.created("Medical record created successfully", response);
  }

  /** Retrieves all medical records relevant to current authenticated user (non-paginated). */
  @GetMapping("/medical-records")
  @Operation(
      summary = "List Medical Records",
      description = "Lists medical records for active Farmer or Veterinarian")
  public ApiResponse<List<MedicalRecordResponse>> listMedicalRecords(Principal principal) {
    List<MedicalRecordResponse> response =
        medicalRecordService.listMedicalRecords(principal.getName());
    return ApiResponse.ok("Medical records retrieved successfully", response);
  }

  /** Retrieves paginated medical records for current authenticated user. */
  @GetMapping("/medical-records/page")
  @Operation(
      summary = "Paginated List of Medical Records",
      description = "Lists medical records with page, size, sort support")
  public ApiResponse<Page<MedicalRecordResponse>> listMedicalRecordsPaginated(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<MedicalRecordResponse> response =
        medicalRecordService.listMedicalRecords(principal.getName(), pageable);
    return ApiResponse.ok("Paginated medical records retrieved successfully", response);
  }

  /** Retrieves a specific medical record by ID. */
  @GetMapping("/medical-records/{id}")
  @Operation(
      summary = "Get Medical Record by ID",
      description = "Returns a single medical record by ID")
  public ApiResponse<MedicalRecordResponse> getMedicalRecordById(
      Principal principal, @PathVariable UUID id) {
    MedicalRecordResponse response =
        medicalRecordService.getMedicalRecordById(principal.getName(), id);
    return ApiResponse.ok("Medical record retrieved successfully", response);
  }

  /** Retrieves complete medical history for a specific livestock animal. */
  @GetMapping("/animals/{animalId}/medical-history")
  @Operation(
      summary = "Get Animal Clinical History",
      description = "Returns medical records timeline for an animal")
  public ApiResponse<List<MedicalRecordResponse>> getAnimalMedicalHistory(
      Principal principal, @PathVariable UUID animalId) {
    List<MedicalRecordResponse> response =
        medicalRecordService.getAnimalMedicalHistory(principal.getName(), animalId);
    return ApiResponse.ok("Animal medical history retrieved successfully", response);
  }

  /** Retrieves the medical record associated with an appointment ID. */
  @GetMapping("/appointments/{appointmentId}/medical-record")
  @Operation(
      summary = "Get Medical Record by Appointment ID",
      description = "Returns medical record linked to appointment")
  public ApiResponse<MedicalRecordResponse> getMedicalRecordByAppointmentId(
      Principal principal, @PathVariable UUID appointmentId) {
    MedicalRecordResponse response =
        medicalRecordService.getMedicalRecordByAppointmentId(principal.getName(), appointmentId);
    return ApiResponse.ok("Appointment medical record retrieved successfully", response);
  }
}
