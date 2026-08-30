package app.vetra.appointment.dto;

import jakarta.validation.constraints.NotBlank;

/** Request payload for sending a chat message or structured treatment instructions. */
public record SendChatMessageRequest(
    @NotBlank(message = "Message content cannot be blank") String content,
    String messageType,
    String treatmentPayloadJson) {}
