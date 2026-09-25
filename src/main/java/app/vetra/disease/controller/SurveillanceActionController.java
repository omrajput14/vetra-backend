package app.vetra.disease.controller;

import app.vetra.disease.service.SurveillanceActionService;
import app.vetra.disease.service.SurveillanceActionService.AlertActionResult;
import app.vetra.disease.service.SurveillanceActionService.ContainmentRequest;
import app.vetra.disease.service.SurveillanceActionService.ContainmentResult;
import app.vetra.infrastructure.response.ApiResponse;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Actions an officer takes from the dashboard. */
@RestController
@RequestMapping("/api/v1/disease")
@PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
public class SurveillanceActionController {

  private final SurveillanceActionService service;

  public SurveillanceActionController(SurveillanceActionService service) {
    this.service = service;
  }

  @PostMapping("/outbreaks/{id}/containment")
  public ApiResponse<ContainmentResult> deployContainment(
      Principal principal, @PathVariable("id") UUID id,
      @RequestBody(required = false) ContainmentRequest request) {
    return ApiResponse.ok("Containment deployed", service.deployContainment(principal.getName(), id, request));
  }

  @PostMapping("/alerts/{id}/acknowledge")
  public ApiResponse<AlertActionResult> acknowledge(
      Principal principal, @PathVariable("id") UUID id,
      @RequestBody(required = false) Map<String, String> body) {
    return ApiResponse.ok("Alert acknowledged",
        service.acknowledge(principal.getName(), id, body != null ? body.get("note") : null));
  }

  @PostMapping("/alerts/{id}/escalate")
  public ApiResponse<AlertActionResult> escalate(
      Principal principal, @PathVariable("id") UUID id,
      @RequestBody(required = false) Map<String, String> body) {
    return ApiResponse.ok("Alert escalated",
        service.escalate(principal.getName(), id, body != null ? body.get("note") : null));
  }
}
