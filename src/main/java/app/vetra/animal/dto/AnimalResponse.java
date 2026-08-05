package app.vetra.animal.dto;

import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Response DTO payload for animal details. */
public record AnimalResponse(
    UUID id,
    UUID farmerId,
    String farmerName,
    String animalName,
    String tagNumber,
    String qrCodeId,
    Species species,
    String breed,
    AnimalGender gender,
    LocalDate birthDate,
    String photoUrl,
    Instant createdAt,
    Instant updatedAt) {}
