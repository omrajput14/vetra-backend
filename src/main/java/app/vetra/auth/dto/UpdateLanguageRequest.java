package app.vetra.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request payload for updating user preferred language. */
public record UpdateLanguageRequest(
    @NotBlank(message = "Language code is required")
    @Pattern(regexp = "^(en|hi|mr)$", message = "Language must be one of: en, hi, mr")
    String language
) {}
