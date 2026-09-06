package app.vetra.appointment.controller;

import app.vetra.appointment.dto.AppointmentLiveLocationResponse;
import app.vetra.appointment.dto.AppointmentResponse;
import app.vetra.appointment.dto.CreateAppointmentRequest;
import app.vetra.appointment.dto.UpdateAppointmentLocationRequest;
import app.vetra.appointment.dto.UpdateAppointmentStatusRequest;
import app.vetra.appointment.service.AppointmentService;
import app.vetra.appointment.service.AppointmentTrackingService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST Controller for managing appointment workflows between Farmers and Veterinarians. */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(
    name = "Appointment Management Module",
    description = "Endpoints for booking, viewing, en-route tracking, and managing veterinary appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;
  private final AppointmentTrackingService appointmentTrackingService;

  /** Constructor injection. */
  public AppointmentController(
      AppointmentService appointmentService,
      AppointmentTrackingService appointmentTrackingService) {
    this.appointmentService = appointmentService;
    this.appointmentTrackingService = appointmentTrackingService;
  }

  /** Creates a new appointment request. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create Appointment",
      description = "Farmer submits a new consultation request for an animal")
  public ApiResponse<AppointmentResponse> createAppointment(
      Principal principal, @Valid @RequestBody CreateAppointmentRequest request) {
    AppointmentResponse response =
        appointmentService.createAppointment(principal.getName(), request);
    return ApiResponse.created("Appointment requested successfully", response);
  }

  /** Lists appointments for the active user (non-paginated). */
  @GetMapping
  @Operation(
      summary = "List Appointments",
      description = "Lists appointment history for the authenticated farmer or veterinarian")
  public ApiResponse<List<AppointmentResponse>> listAppointments(Principal principal) {
    List<AppointmentResponse> response = appointmentService.listAppointments(principal.getName());
    return ApiResponse.ok("Appointments retrieved successfully", response);
  }

  /** Paginated list of appointments for the active user. */
  @GetMapping("/page")
  @Operation(
      summary = "Paginated List of Appointments",
      description = "Retrieves paginated appointments with page, size, sort support")
  public ApiResponse<Page<AppointmentResponse>> listAppointmentsPaginated(
      Principal principal,
      @PageableDefault(size = 20, sort = "appointmentDate", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AppointmentResponse> response =
        appointmentService.listAppointments(principal.getName(), pageable);
    return ApiResponse.ok("Paginated appointments retrieved successfully", response);
  }

  /** Retrieves appointment details by ID. */
  @GetMapping("/{id}")
  @Operation(
      summary = "Get Appointment Details",
      description = "Fetches complete appointment record by UUID")
  public ApiResponse<AppointmentResponse> getAppointmentById(
      Principal principal, @PathVariable("id") UUID id) {
    AppointmentResponse response = appointmentService.getAppointmentById(principal.getName(), id);
    return ApiResponse.ok("Appointment details retrieved successfully", response);
  }

  /** Unified status transition endpoint. */
  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Update Appointment Status",
      description = "Unified status transition endpoint with centralized state machine checks")
  public ApiResponse<AppointmentResponse> updateStatus(
      Principal principal,
      @PathVariable("id") UUID id,
      @Valid @RequestBody UpdateAppointmentStatusRequest request) {
    AppointmentResponse response =
        appointmentService.updateStatus(principal.getName(), id, request);
    return ApiResponse.ok("Appointment status updated successfully", response);
  }

  /** Confirms an appointment (Vet). */
  @PatchMapping("/{id}/confirm")
  @Operation(
      summary = "Confirm Appointment",
      description = "Veterinarian accepts a pending appointment")
  public ApiResponse<AppointmentResponse> confirmAppointment(
      Principal principal, @PathVariable("id") UUID id) {
    AppointmentResponse response = appointmentService.confirmAppointment(principal.getName(), id);
    return ApiResponse.ok("Appointment confirmed successfully", response);
  }

  /** Veterinarian departs for farm visit (EN_ROUTE). */
  @PatchMapping("/{id}/en-route")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Start En-Route Travel",
      description = "Veterinarian signals departure to the farmer's location")
  public ApiResponse<AppointmentResponse> startEnRoute(
      Principal principal, @PathVariable("id") UUID id) {
    AppointmentResponse response = appointmentService.startEnRoute(principal.getName(), id);
    return ApiResponse.ok("Veterinarian is en route", response);
  }

  /** Veterinarian arrives at farmer's location (ARRIVED). */
  @PatchMapping("/{id}/arrive")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Mark Arrival",
      description = "Veterinarian signals arrival at the farmer's location")
  public ApiResponse<AppointmentResponse> markArrived(
      Principal principal, @PathVariable("id") UUID id) {
    AppointmentResponse response = appointmentService.markArrived(principal.getName(), id);
    return ApiResponse.ok("Veterinarian has arrived", response);
  }

  /** Veterinarian streams current GPS coordinates during active EN_ROUTE state. */
  @PostMapping("/{id}/location")
  @PreAuthorize("hasRole('VETERINARIAN')")
  @Operation(
      summary = "Update En-Route Location",
      description = "Veterinarian streams foreground GPS coordinates while en route")
  public ApiResponse<AppointmentLiveLocationResponse> updateLiveLocation(
      Principal principal,
      @PathVariable("id") UUID id,
      @Valid @RequestBody UpdateAppointmentLocationRequest request) {
    AppointmentLiveLocationResponse response =
        appointmentTrackingService.updateLiveLocation(principal.getName(), id, request);
    return ApiResponse.ok("Location updated successfully", response);
  }

  /** Farmer monitors veterinarian en-route progress and GPS distance. */
  @GetMapping("/{id}/location")
  @Operation(
      summary = "Get Live Location",
      description = "Farmer tracks en-route veterinarian location and distance")
  public ApiResponse<AppointmentLiveLocationResponse> getLiveLocation(
      Principal principal, @PathVariable("id") UUID id) {
    AppointmentLiveLocationResponse response =
        appointmentTrackingService.getLiveLocation(principal.getName(), id);
    return ApiResponse.ok("Live location retrieved", response);
  }

  /** Rejects an appointment (Vet). */
  @PatchMapping("/{id}/reject")
  @Operation(
      summary = "Reject Appointment",
      description = "Veterinarian rejects a pending appointment")
  public ApiResponse<AppointmentResponse> rejectAppointment(
      Principal principal,
      @PathVariable("id") UUID id,
      @RequestParam(value = "reason", required = false) String reason) {
    AppointmentResponse response =
        appointmentService.rejectAppointment(principal.getName(), id, reason);
    return ApiResponse.ok("Appointment rejected", response);
  }

  /** Completes an appointment (Vet). */
  @PatchMapping("/{id}/complete")
  @Operation(
      summary = "Complete Appointment",
      description = "Veterinarian completes an appointment with consultation notes")
  public ApiResponse<AppointmentResponse> completeAppointment(
      Principal principal,
      @PathVariable("id") UUID id,
      @RequestParam(value = "notes", required = false) String notes) {
    AppointmentResponse response =
        appointmentService.completeAppointment(principal.getName(), id, notes);
    return ApiResponse.ok("Appointment completed successfully", response);
  }

  /** Cancels an appointment (Farmer). */
  @PatchMapping("/{id}/cancel")
  @Operation(
      summary = "Cancel Appointment",
      description = "Farmer cancels a pending or confirmed appointment")
  public ApiResponse<AppointmentResponse> cancelAppointment(
      Principal principal,
      @PathVariable("id") UUID id,
      @RequestParam(value = "reason", required = false) String reason) {
    AppointmentResponse response =
        appointmentService.cancelAppointment(principal.getName(), id, reason);
    return ApiResponse.ok("Appointment cancelled", response);
  }
}
