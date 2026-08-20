package app.vetra.ai.dto.advisor;

/** Structured candidate condition identified during preliminary veterinary screening. */
public record PossibleConditionDTO(
    String condition,
    Double confidence,
    String reasoning
) {}
