package app.vetra.vaccination.controller;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.vaccination.service.ParaVetDriveService;
import app.vetra.vaccination.service.ParaVetDriveService.DoseResult;
import app.vetra.vaccination.service.ParaVetDriveService.Drive;
import app.vetra.vaccination.service.ParaVetDriveService.DriveAnimal;
import jakarta.validation.constraints.NotEmpty;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Para-vet vaccination drives: open drives nearby, animals to vaccinate, record doses. */
@RestController
@RequestMapping("/api/v1/paravet/drives")
@PreAuthorize("hasRole('PARA_VET')")
public class ParaVetDriveController {

  private final ParaVetDriveService service;

  public ParaVetDriveController(ParaVetDriveService service) {
    this.service = service;
  }

  /** Doses to record: the animals given a dose now. */
  public record DoseRequest(@NotEmpty List<UUID> animalIds) {}

  @GetMapping
  public ApiResponse<List<Drive>> openDrives(Principal principal) {
    return ApiResponse.ok("Open vaccination drives", service.openDrives(principal.getName()));
  }

  @GetMapping("/{id}/animals")
  public ApiResponse<List<DriveAnimal>> animals(Principal principal, @PathVariable("id") UUID id) {
    return ApiResponse.ok("Animals in the drive area", service.animalsToVaccinate(principal.getName(), id));
  }

  @PostMapping("/{id}/doses")
  public ApiResponse<DoseResult> recordDoses(
      Principal principal, @PathVariable("id") UUID id,
      @jakarta.validation.Valid @RequestBody DoseRequest request) {
    return ApiResponse.ok("Doses recorded", service.recordDoses(principal.getName(), id, request.animalIds()));
  }
}
