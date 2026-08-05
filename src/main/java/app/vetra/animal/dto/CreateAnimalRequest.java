package app.vetra.animal.dto;

import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Request payload DTO for creating a new animal record. */
public record CreateAnimalRequest(
    String animalName,
    @NotBlank String tagNumber,
    String qrCodeId,
    @NotNull Species species,
    String breed,
    @NotNull AnimalGender gender,
    LocalDate birthDate,
    String photoUrl) {}
