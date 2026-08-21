package app.vetra.auth.controller;

import app.vetra.auth.dto.UpdateLanguageRequest;
import app.vetra.auth.dto.UpdateLanguageResponse;
import app.vetra.auth.dto.UserProfileDto;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing user preferences endpoints including localization language selection.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Preferences", description = "Endpoints for managing user settings and localization preferences")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final AuthService authService;

  /** Constructor injection. */
  public UserController(AuthService authService) {
    this.authService = authService;
  }

  /** Updates preferred language for the authenticated user. */
  @PutMapping("/preferences/language")
  @Operation(
      summary = "Update Preferred Language",
      description = "Updates the user's preferred language (en, hi, mr) for localized UI and AI responses")
  public ApiResponse<UpdateLanguageResponse> updateLanguagePreference(
      Principal principal, @Valid @RequestBody UpdateLanguageRequest request) {
    UserProfileDto updatedProfile =
        authService.updateUserLanguagePreference(principal.getName(), request.language());
    UpdateLanguageResponse response =
        new UpdateLanguageResponse(true, updatedProfile.preferredLanguage());
    return ApiResponse.ok("Preferred language updated successfully", response);
  }
}
