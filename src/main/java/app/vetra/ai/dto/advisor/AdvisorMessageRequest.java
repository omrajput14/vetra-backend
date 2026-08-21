package app.vetra.ai.dto.advisor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request payload for sending an owner message to an active advisor session. */
public record AdvisorMessageRequest(
    @NotBlank(message = "Message content must not be blank")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    String message,
    String preferredLanguage
) {
  public AdvisorMessageRequest(String message) {
    this(message, null);
  }
}
