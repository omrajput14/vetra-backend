package app.vetra.appointment.service;

import app.vetra.appointment.dto.AppointmentLiveLocationResponse;
import app.vetra.appointment.dto.UpdateAppointmentLocationRequest;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing live GPS location streaming and proximity calculations for appointments
 * in the active EN_ROUTE state.
 */
@Service
public class AppointmentTrackingService {

  private final AppointmentRepository appointmentRepository;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;

  /** Constructor injection. */
  public AppointmentTrackingService(
      AppointmentRepository appointmentRepository,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository) {
    this.appointmentRepository = appointmentRepository;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
  }

  /** Updates veterinarian live coordinates during an active EN_ROUTE appointment. */
  @Transactional
  public AppointmentLiveLocationResponse updateLiveLocation(
      String currentUserIdentifier, UUID id, UpdateAppointmentLocationRequest request) {
    User user = getUserByEmail(currentUserIdentifier);
    if (user.getRole() != UserRole.VETERINARIAN) {
      throw new UnauthorizedResourceAccessException(
          "Only veterinarians can update live location", "AUTH_006");
    }

    Appointment appointment = getAppointment(id);
    validateUserAccess(user, appointment);

    if (appointment.getStatus() != AppointmentStatus.EN_ROUTE) {
      throw new BusinessRuleException(
          "Location updates are only permitted when appointment is EN_ROUTE", "APPT_006");
    }

    appointment.setVetLatitude(request.latitude());
    appointment.setVetLongitude(request.longitude());
    appointment.setVetLocationUpdatedAt(Instant.now());
    appointmentRepository.save(appointment);

    Double distanceKm = calculateDistanceToFarmer(appointment);

    return AppointmentLiveLocationResponse.live(
        request.latitude(),
        request.longitude(),
        distanceKm,
        appointment.getStatus().name(),
        appointment.getVetLocationUpdatedAt());
  }

  /** Retrieves live location of veterinarian for an appointment. */
  @Transactional(readOnly = true)
  public AppointmentLiveLocationResponse getLiveLocation(String currentUserIdentifier, UUID id) {
    User user = getUserByEmail(currentUserIdentifier);
    Appointment appointment = getAppointment(id);
    validateUserAccess(user, appointment);

    if (appointment.getStatus() != AppointmentStatus.EN_ROUTE) {
      return AppointmentLiveLocationResponse.notLive(
          appointment.getStatus().name(), "Appointment is not currently en route");
    }

    if (appointment.getVetLatitude() == null || appointment.getVetLongitude() == null) {
      return AppointmentLiveLocationResponse.notLive(
          appointment.getStatus().name(), "Veterinarian location not yet available");
    }

    Double distanceKm = calculateDistanceToFarmer(appointment);

    return AppointmentLiveLocationResponse.live(
        appointment.getVetLatitude(),
        appointment.getVetLongitude(),
        distanceKm,
        appointment.getStatus().name(),
        appointment.getVetLocationUpdatedAt());
  }

  private Appointment getAppointment(UUID id) {
    return appointmentRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Appointment not found with ID: " + id, "APPT_001"));
  }

  private void validateUserAccess(User user, Appointment appointment) {
    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer =
          farmerProfileRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      if (!appointment.getFarmer().getId().equals(farmer.getId())) {
        throw new UnauthorizedResourceAccessException(
            "Unauthorized access to farmer appointment", "APPT_002");
      }
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      VetProfile vet =
          vetProfileRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new ResourceNotFoundException("Vet profile not found", "USER_004"));
      if (!appointment.getVeterinarian().getId().equals(vet.getId())) {
        throw new UnauthorizedResourceAccessException(
            "Unauthorized access to veterinarian appointment", "APPT_003");
      }
    }
  }

  private Double calculateDistanceToFarmer(Appointment appointment) {
    if (appointment.getVetLatitude() == null
        || appointment.getVetLongitude() == null
        || appointment.getFarmer() == null
        || appointment.getFarmer().getLatitude() == null
        || appointment.getFarmer().getLongitude() == null) {
      return null;
    }
    return haversineDistance(
        appointment.getVetLatitude(),
        appointment.getVetLongitude(),
        appointment.getFarmer().getLatitude(),
        appointment.getFarmer().getLongitude());
  }

  private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
    final double earthRadiusKm = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(earthRadiusKm * c * 100.0) / 100.0;
  }

  private User getUserByEmail(String email) {
    return userRepository
        .findByIdentifier(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email, "USER_004"));
  }
}
