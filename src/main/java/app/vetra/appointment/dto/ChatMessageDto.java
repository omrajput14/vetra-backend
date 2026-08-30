package app.vetra.appointment.dto;

import app.vetra.infrastructure.persistence.entity.AppointmentChatMessage;
import java.time.Instant;
import java.util.UUID;

/** Response DTO representing an appointment consultation message. */
public record ChatMessageDto(
    UUID id,
    UUID appointmentId,
    UUID senderId,
    String senderName,
    String senderRole,
    UUID recipientId,
    String messageType,
    String content,
    String treatmentPayloadJson,
    boolean isRead,
    Instant createdAt) {

  public static ChatMessageDto fromEntity(AppointmentChatMessage entity) {
    String roleStr = entity.getSender() != null && entity.getSender().getRole() != null
        ? entity.getSender().getRole().name()
        : "UNKNOWN";
    String senderName = "User";
    if (entity.getSender() != null) {
      senderName = entity.getSender().getEmail() != null ? entity.getSender().getEmail() : "User";
    }

    return new ChatMessageDto(
        entity.getId(),
        entity.getAppointment() != null ? entity.getAppointment().getId() : null,
        entity.getSender() != null ? entity.getSender().getId() : null,
        senderName,
        roleStr,
        entity.getRecipient() != null ? entity.getRecipient().getId() : null,
        entity.getMessageType(),
        entity.getContent(),
        entity.getTreatmentPayloadJson(),
        entity.isRead(),
        entity.getCreatedAt());
  }
}
