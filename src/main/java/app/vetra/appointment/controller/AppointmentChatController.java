package app.vetra.appointment.controller;

import app.vetra.appointment.dto.ChatMessageDto;
import app.vetra.appointment.dto.SendChatMessageRequest;
import app.vetra.appointment.service.AppointmentChatService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing consultation chat and treatment instructions endpoints. */
@RestController
@RequestMapping("/api/v1/appointments/{appointmentId}/messages")
@Tag(
    name = "Appointment Consultation Chat Module",
    description = "Direct communication and treatment instructions between farmer and veterinarian")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentChatController {

  private final AppointmentChatService chatService;

  /** Constructor injection. */
  public AppointmentChatController(AppointmentChatService chatService) {
    this.chatService = chatService;
  }

  /** Sends a message or treatment instruction within an appointment. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Send Consultation Message or Treatment Instruction",
      description = "Sends a chat message or structured treatment instruction for an appointment.")
  public ApiResponse<ChatMessageDto> sendMessage(
      Principal principal,
      @PathVariable("appointmentId") UUID appointmentId,
      @Valid @RequestBody SendChatMessageRequest request) {
    ChatMessageDto response = chatService.sendMessage(principal.getName(), appointmentId, request);
    return ApiResponse.created("Message sent successfully", response);
  }

  /** Retrieves consultation messages for an appointment. */
  @GetMapping
  @Operation(
      summary = "Get Consultation Messages",
      description = "Retrieves all consultation messages for an appointment in chronological order.")
  public ApiResponse<List<ChatMessageDto>> getMessages(
      Principal principal, @PathVariable("appointmentId") UUID appointmentId) {
    List<ChatMessageDto> response = chatService.getMessages(principal.getName(), appointmentId);
    return ApiResponse.ok("Messages retrieved successfully", response);
  }
}
