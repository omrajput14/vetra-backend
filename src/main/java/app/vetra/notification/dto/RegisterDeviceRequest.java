package app.vetra.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for registering a push device token.
 */
public record RegisterDeviceRequest(
    @NotBlank(message = "Device token is required")
    @Size(max = 512, message = "Device token cannot exceed 512 characters")
    String deviceToken,

    @Size(max = 32, message = "Platform string cannot exceed 32 characters")
    String platform,

    @Size(max = 32, message = "App version string cannot exceed 32 characters")
    String appVersion
) {}
