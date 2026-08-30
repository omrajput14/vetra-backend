package app.vetra.auth.controller;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.ChangePasswordRequest;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.dto.RefreshTokenRequest;
import app.vetra.auth.dto.UserProfileDto;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication REST Controller exposing Farmer and Vet sign-in/registration, session refresh,
 * logout, password change, and current user profile endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Authentication Module",
    description = "Farmer & Vet registration, login, refresh, and profile endpoints")
public class AuthController {

  private final AuthService authService;

  /** Constructor injection. */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Registers a new farmer account. */
  @PostMapping("/farmer/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Register Farmer Account",
      description = "Creates User and FarmerProfile, returning JWT access and refresh tokens")
  public ApiResponse<AuthResponse> registerFarmer(
      @Valid @RequestBody FarmerRegisterRequest request) {
    AuthResponse response = authService.registerFarmer(request);
    return ApiResponse.created("Farmer registered successfully", response);
  }

  /** Registers a new veterinarian account. */
  @PostMapping("/vet/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Register Veterinarian Account",
      description = "Creates User and VetProfile, returning JWT access and refresh tokens")
  public ApiResponse<AuthResponse> registerVet(@Valid @RequestBody VetRegisterRequest request) {
    AuthResponse response = authService.registerVet(request);
    return ApiResponse.created("Veterinarian registered successfully", response);
  }

  /** Authenticates farmer credentials. */
  @PostMapping("/farmer/login")
  @Operation(
      summary = "Farmer Login",
      description = "Authenticates farmer credentials and returns JWT tokens")
  public ApiResponse<AuthResponse> loginFarmer(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.loginFarmer(request);
    return ApiResponse.ok("Farmer login successful", response);
  }

  /** Authenticates veterinarian credentials. */
  @PostMapping("/vet/login")
  @Operation(
      summary = "Veterinarian Login",
      description = "Authenticates veterinarian credentials and returns JWT tokens")
  public ApiResponse<AuthResponse> loginVet(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.loginVet(request);
    return ApiResponse.ok("Veterinarian login successful", response);
  }

  /** Authenticates user credentials for any system role (Government, Admin, Vet, Farmer). */
  @PostMapping("/login")
  @Operation(
      summary = "System User Login",
      description = "Authenticates user credentials across roles and returns JWT tokens")
  public ApiResponse<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.loginUser(request);
    return ApiResponse.ok("Login successful", response);
  }

  /** Refreshes JWT access token using valid refresh token. */
  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh Access Token",
      description = "Generates new JWT access and refresh tokens using valid refresh token")
  public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse response = authService.refreshToken(request);
    return ApiResponse.ok("Token refreshed successfully", response);
  }

  /** Revokes refresh token session. */
  @PostMapping("/logout")
  @Operation(summary = "Logout Session", description = "Revokes refresh token for stateless logout")
  public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request.refreshToken());
    return ApiResponse.ok("Logged out successfully", null);
  }

  /** Changes password for current authenticated user. */
  @PostMapping("/change-password")
  @Operation(summary = "Change Password", description = "Updates password for authenticated user")
  public ApiResponse<Void> changePassword(
      Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(principal.getName(), request);
    return ApiResponse.ok("Password changed successfully", null);
  }

  /** Returns current authenticated user profile. */
  @GetMapping("/me")
  @Operation(
      summary = "Get Current Authenticated User Profile",
      description = "Returns active user profile and role details")
  public ApiResponse<UserProfileDto> getCurrentUser(Principal principal) {
    UserProfileDto response = authService.getCurrentUserProfileDtoByIdentifier(principal.getName());
    return ApiResponse.ok("User profile retrieved successfully", response);
  }

  /** Updates active user profile details. */
  @PutMapping("/profile")
  @Operation(summary = "Update Profile", description = "Updates details of active user profile")
  public ApiResponse<UserProfileDto> updateProfile(
      Principal principal, @Valid @RequestBody app.vetra.auth.dto.UpdateProfileRequest request) {
    UserProfileDto response = authService.updateUserProfile(principal.getName(), request);
    return ApiResponse.ok("Profile updated successfully", response);
  }

  /** Lists or discovers registered veterinarians by proximity and administrative location. */
  @GetMapping("/vets")
  @Operation(
      summary = "List or Search Nearby Veterinarians",
      description =
          "Returns directory of registered veterinarians with hierarchical proximity matching"
              + " (GPS radius, village, taluka, district)")
  public ApiResponse<java.util.List<app.vetra.auth.dto.VetSummaryDto>> listVets(
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude,
      @RequestParam(required = false, defaultValue = "50.0") Double radiusKm,
      @RequestParam(required = false) String village,
      @RequestParam(required = false) String taluka,
      @RequestParam(required = false) String district) {
    if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
      throw new app.vetra.infrastructure.exception.BusinessRuleException(
          "Latitude must be between -90.0 and 90.0", "LOC_001");
    }
    if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
      throw new app.vetra.infrastructure.exception.BusinessRuleException(
          "Longitude must be between -180.0 and 180.0", "LOC_002");
    }
    if (radiusKm != null && radiusKm <= 0) {
      throw new app.vetra.infrastructure.exception.BusinessRuleException(
          "Radius must be greater than 0", "LOC_003");
    }
    java.util.List<app.vetra.auth.dto.VetSummaryDto> response =
        authService.searchNearbyVeterinarians(
            latitude, longitude, radiusKm, village, taluka, district);
    return ApiResponse.ok("Veterinarians retrieved successfully", response);
  }
}
