package app.vetra.notification.controller;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.notification.dto.NotificationPreferenceResponse;
import app.vetra.notification.dto.UpdatePreferenceRequest;
import app.vetra.notification.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing end-user notification channel preferences.
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Tag(name = "Notification Preference Management", description = "Endpoints for viewing and updating user notification opt-in preferences")
@SecurityRequirement(name = "bearerAuth")
public class NotificationPreferenceController {

  private final NotificationPreferenceService preferenceService;

  /** Constructor injection. */
  public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
    this.preferenceService = preferenceService;
  }

  /** Retrieves user notification preferences. */
  @GetMapping
  @Operation(summary = "Get Notification Preferences", description = "Retrieves end-user notification opt-in/opt-out channel preferences.")
  public ApiResponse<NotificationPreferenceResponse> getPreferences(Principal principal) {
    NotificationPreferenceResponse response = preferenceService.getPreferences(principal.getName());
    return ApiResponse.ok("Notification preferences retrieved successfully", response);
  }

  /** Updates user notification preferences. */
  @PutMapping
  @Operation(summary = "Update Notification Preferences", description = "Updates end-user notification opt-in/opt-out channel preferences.")
  public ApiResponse<NotificationPreferenceResponse> updatePreferences(
      Principal principal, @RequestBody UpdatePreferenceRequest request) {
    NotificationPreferenceResponse response = preferenceService.updatePreferences(principal.getName(), request);
    return ApiResponse.ok("Notification preferences updated successfully", response);
  }
}
