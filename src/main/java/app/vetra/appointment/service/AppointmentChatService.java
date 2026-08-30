package app.vetra.appointment.service;

import app.vetra.appointment.dto.ChatMessageDto;
import app.vetra.appointment.dto.SendChatMessageRequest;
import app.vetra.appointment.repository.AppointmentChatMessageRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.AppointmentChatMessage;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service managing direct consultation messaging between assigned farmer and veterinarian. */
@Service
public class AppointmentChatService {

  private final AppointmentChatMessageRepository chatMessageRepository;
  private final AppointmentRepository appointmentRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  /** Constructor injection. */
  public AppointmentChatService(
      AppointmentChatMessageRepository chatMessageRepository,
      AppointmentRepository appointmentRepository,
      UserRepository userRepository,
      NotificationService notificationService) {
    this.chatMessageRepository = chatMessageRepository;
    this.appointmentRepository = appointmentRepository;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  /** Sends a chat message or treatment instruction for an appointment. */
  @Transactional
  public ChatMessageDto sendMessage(
      String currentUserIdentifier, UUID appointmentId, SendChatMessageRequest request) {
    User sender = userRepository
        .findByIdentifier(currentUserIdentifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));

    Appointment appointment = appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found", "APPT_001"));

    User farmerUser = appointment.getFarmer().getUser();
    User vetUser = appointment.getVeterinarian().getUser();

    boolean isFarmer = sender.getId().equals(farmerUser.getId());
    boolean isVet = sender.getId().equals(vetUser.getId());

    if (!isFarmer && !isVet && sender.getRole() != UserRole.ADMINISTRATOR) {
      throw new UnauthorizedResourceAccessException(
          "Only the assigned farmer or veterinarian can send consultation messages", "CHAT_001");
    }

    String msgType = request.messageType() != null && !request.messageType().isBlank()
        ? request.messageType().toUpperCase()
        : "TEXT";

    if ("TREATMENT_INSTRUCTION".equalsIgnoreCase(msgType) && isFarmer) {
      throw new UnauthorizedResourceAccessException(
          "Only licensed veterinarians can author structured treatment instructions",
          "CHAT_002");
    }

    User recipient = isFarmer ? vetUser : farmerUser;

    AppointmentChatMessage message = AppointmentChatMessage.builder()
        .appointment(appointment)
        .sender(sender)
        .recipient(recipient)
        .messageType(msgType)
        .content(request.content().trim())
        .treatmentPayloadJson(request.treatmentPayloadJson())
        .isRead(false)
        .build();

    AppointmentChatMessage saved = chatMessageRepository.save(message);
    dispatchChatNotification(appointment, recipient, isFarmer, msgType, request.content().trim(), appointmentId);
    return ChatMessageDto.fromEntity(saved);
  }

  private void dispatchChatNotification(
      Appointment appointment,
      User recipient,
      boolean isFarmer,
      String msgType,
      String content,
      UUID appointmentId) {
    if (notificationService == null) {
      return;
    }
    try {
      boolean isTreatment = "TREATMENT_INSTRUCTION".equalsIgnoreCase(msgType);
      String vetName = appointment.getVeterinarian().getFullName();
      String farmerName = appointment.getFarmer().getFullName();
      String notifTitle = isTreatment
          ? "Treatment Instructions from Dr. " + vetName
          : "New message from " + (isFarmer ? farmerName : "Dr. " + vetName);

      String animalDesc = "livestock";
      if (appointment.getAnimal() != null && appointment.getAnimal().getAnimalName() != null) {
        animalDesc = appointment.getAnimal().getAnimalName();
      }

      String notifBody = isTreatment
          ? "Prescribed clinical instructions received for " + animalDesc
          : content;

      notificationService.sendNotification(
          recipient.getId(),
          notifTitle,
          notifBody,
          "{\"appointmentId\":\"" + appointmentId + "\",\"route\":\"/appointment-chat?id=" + appointmentId + "\"}",
          NotificationChannel.PUSH,
          NotificationPriority.HIGH);
    } catch (Exception e) {
      // Non-blocking notification dispatch
    }
  }

  /** Retrieves chronological message history for an appointment. */
  @Transactional
  public List<ChatMessageDto> getMessages(String currentUserIdentifier, UUID appointmentId) {
    User user = userRepository
        .findByIdentifier(currentUserIdentifier)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_004"));

    Appointment appointment = appointmentRepository
        .findById(appointmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found", "APPT_001"));

    User farmerUser = appointment.getFarmer().getUser();
    User vetUser = appointment.getVeterinarian().getUser();

    boolean isFarmer = user.getId().equals(farmerUser.getId());
    boolean isVet = user.getId().equals(vetUser.getId());

    if (!isFarmer && !isVet && user.getRole() != UserRole.ADMINISTRATOR) {
      throw new UnauthorizedResourceAccessException(
          "Only the assigned farmer or veterinarian can view consultation messages", "CHAT_001");
    }

    List<AppointmentChatMessage> messages =
        chatMessageRepository.findByAppointmentOrderByCreatedAtAsc(appointment);

    // Mark unread messages directed to current user as read
    for (AppointmentChatMessage msg : messages) {
      if (!msg.isRead() && msg.getRecipient() != null && msg.getRecipient().getId().equals(user.getId())) {
        msg.setRead(true);
        chatMessageRepository.save(msg);
      }
    }

    return messages.stream().map(ChatMessageDto::fromEntity).toList();
  }
}
