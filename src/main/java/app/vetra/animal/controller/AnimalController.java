package app.vetra.animal.controller;

import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.dto.UpdateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing livestock animals.
 */
@RestController
@RequestMapping("/api/v1/animals")
@Tag(name = "Animal Management Module", description = "Endpoints for creating, listing, viewing, updating, and searching livestock animals")
public class AnimalController {

  private final AnimalService animalService;

  /** Constructor injection. */
  public AnimalController(AnimalService animalService) {
    this.animalService = animalService;
  }

  /** Creates a new animal record. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create Animal", description = "Registers a new livestock animal for the authenticated farmer")
  public ApiResponse<AnimalResponse> createAnimal(
      Principal principal, @Valid @RequestBody CreateAnimalRequest request) {
    AnimalResponse response = animalService.createAnimal(principal.getName(), request);
    return ApiResponse.created("Animal registered successfully", response);
  }

  /** Lists animals owned by farmer or all animals for vets/admins (non-paginated). */
  @GetMapping
  @Operation(summary = "List Animals", description = "Retrieves animals based on active user role")
  public ApiResponse<List<AnimalResponse>> listAnimals(Principal principal) {
    List<AnimalResponse> response = animalService.listAnimals(principal.getName());
    return ApiResponse.ok("Animals retrieved successfully", response);
  }

  /** Paginated list of animals based on user role. */
  @GetMapping("/page")
  @Operation(summary = "Paginated List of Animals", description = "Retrieves paginated animals with page, size, sort support")
  public ApiResponse<Page<AnimalResponse>> listAnimalsPaginated(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<AnimalResponse> response = animalService.listAnimals(principal.getName(), pageable);
    return ApiResponse.ok("Paginated animals retrieved successfully", response);
  }

  /** Retrieves an animal record by ID. */
  @GetMapping("/{id}")
  @Operation(summary = "Get Animal Details", description = "Fetches an animal record by UUID")
  public ApiResponse<AnimalResponse> getAnimalById(
      Principal principal, @PathVariable("id") UUID id) {
    AnimalResponse response = animalService.getAnimalById(principal.getName(), id);
    return ApiResponse.ok("Animal details retrieved successfully", response);
  }

  /** Updates an existing animal record. */
  @PutMapping("/{id}")
  @Operation(summary = "Update Animal", description = "Updates details of an existing animal record")
  public ApiResponse<AnimalResponse> updateAnimal(
      Principal principal,
      @PathVariable("id") UUID id,
      @Valid @RequestBody UpdateAnimalRequest request) {
    AnimalResponse response = animalService.updateAnimal(principal.getName(), id, request);
    return ApiResponse.ok("Animal details updated successfully", response);
  }

  /** Deletes an animal record by ID. */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete Animal", description = "Removes an animal record")
  public ApiResponse<Void> deleteAnimal(Principal principal, @PathVariable("id") UUID id) {
    animalService.deleteAnimal(principal.getName(), id);
    return ApiResponse.ok("Animal deleted successfully", null);
  }

  /** Searches animals with optional filters. */
  @GetMapping("/search")
  @Operation(summary = "Search Animals", description = "Filters animals by name, tag number, QR code, species, breed, or gender")
  public ApiResponse<List<AnimalResponse>> searchAnimals(
      Principal principal,
      @RequestParam(value = "animalName", required = false) String animalName,
      @RequestParam(value = "tagNumber", required = false) String tagNumber,
      @RequestParam(value = "qrCodeId", required = false) String qrCodeId,
      @RequestParam(value = "species", required = false) Species species,
      @RequestParam(value = "breed", required = false) String breed,
      @RequestParam(value = "gender", required = false) AnimalGender gender) {

    List<AnimalResponse> response = animalService.searchAnimals(
        principal.getName(), animalName, tagNumber, qrCodeId, species, breed, gender);
    return ApiResponse.ok("Search results retrieved successfully", response);
  }
}
