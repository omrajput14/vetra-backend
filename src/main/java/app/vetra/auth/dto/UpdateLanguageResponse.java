package app.vetra.auth.dto;

/** Response payload for user preferred language updates. */
public record UpdateLanguageResponse(
    boolean success,
    String preferredLanguage
) {}
