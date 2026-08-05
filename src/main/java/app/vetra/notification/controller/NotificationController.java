package app.vetra.notification.controller;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.notification.dto.NotificationResponse;
import app.vetra.notification.dto.UnreadCountResponse;
import app.vetra.notification.entity.NotificationTemplate;
import app.vetra.notification.repository.NotificationTemplateRepository;
import app.vetra.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller providing inbox query, read status updates, and template lookup endpoints. */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(
    name = "Notification Inbox & Delivery Platform",
    description = "Endpoints for viewing user inbox notifications, unread counts, and templates")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

  private final NotificationService notificationService;
  private final NotificationTemplateRepository templateRepository;

  /** Constructor injection. */
  public NotificationController(
      NotificationService notificationService, NotificationTemplateRepository templateRepository) {
    this.notificationService = notificationService;
    this.templateRepository = templateRepository;
  }

  /** Lists user notifications with pagination. */
  @GetMapping
  @Operation(
      summary = "List User Notifications",
      description = "Retrieves paginated inbox notifications for the authenticated user.")
  public ApiResponse<Page<NotificationResponse>> listNotifications(
      Principal principal,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<NotificationResponse> response =
        notificationService.listUserNotifications(principal.getName(), pageable);
    return ApiResponse.ok("Notifications retrieved successfully", response);
  }

  /** Gets unread notification count. */
  @GetMapping("/unread")
  @Operation(
      summary = "Get Unread Notification Count",
      description = "Returns total count of unread notifications for the authenticated user.")
  public ApiResponse<UnreadCountResponse> getUnreadCount(Principal principal) {
    UnreadCountResponse response = notificationService.getUnreadCount(principal.getName());
    return ApiResponse.ok("Unread notification count retrieved successfully", response);
  }

  /** Marks a notification as read. */
  @PatchMapping("/{id}/read")
  @Operation(
      summary = "Mark Notification as Read",
      description = "Marks a specific notification as read by UUID.")
  public ApiResponse<NotificationResponse> markAsRead(
      Principal principal, @PathVariable("id") UUID id) {
    NotificationResponse response = notificationService.markAsRead(principal.getName(), id);
    return ApiResponse.ok("Notification marked as read successfully", response);
  }

  /** Lists system notification templates. */
  @GetMapping("/templates")
  @Operation(
      summary = "List System Notification Templates",
      description = "Retrieves available system notification templates.")
  public ApiResponse<List<NotificationTemplate>> listTemplates() {
    List<NotificationTemplate> response = templateRepository.findAll();
    return ApiResponse.ok("Notification templates retrieved successfully", response);
  }
}
