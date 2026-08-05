package app.vetra.notification.controller;

import app.vetra.infrastructure.response.ApiResponse;
import app.vetra.notification.dto.RegisterDeviceRequest;
import app.vetra.notification.entity.NotificationDevice;
import app.vetra.notification.service.DeviceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller managing mobile and web device registration for push notifications. */
@RestController
@RequestMapping("/api/v1/notifications/devices")
@Tag(
    name = "Notification Device Management",
    description = "Endpoints for registering and updating mobile push device tokens")
@SecurityRequirement(name = "bearerAuth")
public class DeviceRegistrationController {

  private final DeviceManagementService deviceService;

  /** Constructor injection. */
  public DeviceRegistrationController(DeviceManagementService deviceService) {
    this.deviceService = deviceService;
  }

  /** Registers a push device token. */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Register Push Device Token",
      description = "Registers a new device token for Firebase push notification delivery.")
  public ApiResponse<String> registerDevice(
      Principal principal, @Valid @RequestBody RegisterDeviceRequest request) {
    NotificationDevice device = deviceService.registerDevice(principal.getName(), request);
    return ApiResponse.created("Push device registered successfully", device.getId().toString());
  }

  /** Deactivates a device token. */
  @PutMapping("/{id}")
  @Operation(
      summary = "Deactivate Push Device Token",
      description = "Deactivates a registered device token.")
  public ApiResponse<Void> deactivateDevice(Principal principal, @PathVariable("id") UUID id) {
    deviceService.deactivateDevice(principal.getName(), id);
    return ApiResponse.ok("Device token deactivated successfully", null);
  }
}
