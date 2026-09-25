package app.vetra.disease.controller;

import app.vetra.disease.service.WorkforceService;
import app.vetra.disease.service.WorkforceService.Member;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.infrastructure.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Officers: field workforce (vets and para-vets) and para-vet approval. */
@RestController
@RequestMapping("/api/v1/workforce")
@PreAuthorize("hasAnyRole('GOVERNMENT_OFFICER', 'ADMINISTRATOR')")
public class WorkforceController {

  private final WorkforceService service;

  public WorkforceController(WorkforceService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<Member>> list() {
    return ApiResponse.ok("Field workforce", service.list());
  }

  @PostMapping("/para-vets/{userId}/approve")
  public ApiResponse<Member> approve(@PathVariable("userId") UUID userId) {
    return ApiResponse.ok("Para-vet approved", service.setParaVetStatus(userId, VerificationStatus.VERIFIED));
  }

  @PostMapping("/para-vets/{userId}/reject")
  public ApiResponse<Member> reject(@PathVariable("userId") UUID userId) {
    return ApiResponse.ok("Para-vet rejected", service.setParaVetStatus(userId, VerificationStatus.REJECTED));
  }
}
