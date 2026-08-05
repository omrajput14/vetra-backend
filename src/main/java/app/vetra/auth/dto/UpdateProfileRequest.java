package app.vetra.auth.dto;

/** Request payload for updating active user profile information. */
public record UpdateProfileRequest(
    String fullName,
    String phone,
    String farmName,
    String village,
    String district,
    String state,
    String clinicName,
    String specialization,
    String qualification,
    Integer yearsExperience) {}
