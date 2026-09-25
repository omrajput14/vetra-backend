package app.vetra.ai.event;

import java.util.UUID;

/** A para-vet checked an AI scan in the field and sent it to a vet. Farm location may be null. */
public record AIScanEscalatedEvent(
    UUID scanId,
    UUID farmerUserId,
    String animalName,
    String diagnosis,
    Double farmLatitude,
    Double farmLongitude) {}
