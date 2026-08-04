package app.vetra.appointment.service;

import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.dto.AppointmentResponse;
import app.vetra.appointment.dto.CreateAppointmentRequest;
import app.vetra.appointment.dto.UpdateAppointmentStatusRequest;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.cache.CacheNames;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.metrics.VetraMetrics;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.UserRole;
import io.micrometer.tracing.Tracer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing appointment lifecycle and business state machine.
 */
@Service
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final UserRepository userRepository;
  private final FarmerProfileRepository farmerProfileRepository;
  private final VetProfileRepository vetProfileRepository;
  private final AnimalRepository animalRepository;
  private final VetraMetrics vetraMetrics;
  private final Tracer tracer;

  /** Constructor injection. */
  public AppointmentService(
      AppointmentRepository appointmentRepository,
      UserRepository userRepository,
      FarmerProfileRepository farmerProfileRepository,
      VetProfileRepository vetProfileRepository,
      AnimalRepository animalRepository,
      VetraMetrics vetraMetrics,
      Tracer tracer) {
    this.appointmentRepository = appointmentRepository;
    this.userRepository = userRepository;
    this.farmerProfileRepository = farmerProfileRepository;
    this.vetProfileRepository = vetProfileRepository;
    this.animalRepository = animalRepository;
    this.vetraMetrics = vetraMetrics;
    this.tracer = tracer;
  }

  /** Creates a new appointment for the authenticated farmer. */
  @Transactional
  @CacheEvict(value = {CacheNames.DASHBOARD_FARMER, CacheNames.DASHBOARD_VET}, allEntries = true)
  public AppointmentResponse createAppointment(
      String currentUserIdentifier, CreateAppointmentRequest request) {
    User user = getUserByEmail(currentUserIdentifier);
    if (user.getRole() != UserRole.FARMER) {
      throw new UnauthorizedResourceAccessException("Only registered farmers can request appointments", "AUTH_006");
    }

    FarmerProfile farmer = farmerProfileRepository.findByUser(user)
        .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));

    if (request.appointmentDate().isBefore(LocalDate.now())) {
      throw new BusinessRuleException("Appointment date cannot be in the past", "APPT_007");
    }

    Animal animal = animalRepository.findById(request.animalId())
        .orElseThrow(() -> new ResourceNotFoundException("Animal not found with ID: " + request.animalId(), "ANIMAL_001"));

    if (!animal.getFarmer().getId().equals(farmer.getId())) {
      throw new UnauthorizedResourceAccessException("Animal does not belong to the requesting farmer", "ANIMAL_002");
    }

    VetProfile vet = vetProfileRepository.findById(request.veterinarianId())
        .orElseThrow(() -> new ResourceNotFoundException("Veterinarian profile not found", "USER_004"));

    Appointment appointment = Appointment.builder()
        .farmer(farmer)
        .veterinarian(vet)
        .animal(animal)
        .appointmentDate(request.appointmentDate())
        .appointmentTime(request.appointmentTime())
        .visitType(request.visitType())
        .reason(request.reason())
        .status(AppointmentStatus.PENDING)
        .build();

    Appointment saved = appointmentRepository.save(appointment);
    vetraMetrics.recordAppointmentCreated();

    // Tag span with appointment visit type — enum value, bounded cardinality, no PII.
    if (tracer.currentSpan() != null && request.visitType() != null) {
      tracer.currentSpan().tag("appointment.visit_type", request.visitType().name());
    }

    return AppointmentResponse.fromEntity(saved);
  }

  /** Retrieves all appointments relevant to the current user (non-paginated). */
  @Transactional(readOnly = true)
  public List<AppointmentResponse> listAppointments(String currentUserIdentifier) {
    User user = getUserByEmail(currentUserIdentifier);

    List<Appointment> appointments;
    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      appointments = appointmentRepository.findByFarmerOrderByAppointmentDateDescAppointmentTimeDesc(farmer);
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      VetProfile vet = vetProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found", "USER_004"));
      appointments = appointmentRepository.findByVeterinarianOrderByAppointmentDateDescAppointmentTimeDesc(vet);
    } else {
      appointments = appointmentRepository.findAll();
    }

    return appointments.stream().map(AppointmentResponse::fromEntity).toList();
  }

  /** Retrieves appointments relevant to the current user with Pageable. */
  @Transactional(readOnly = true)
  public Page<AppointmentResponse> listAppointments(String currentUserIdentifier, Pageable pageable) {
    User user = getUserByEmail(currentUserIdentifier);

    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      return appointmentRepository.findByFarmer(farmer, pageable).map(AppointmentResponse::fromEntity);
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      VetProfile vet = vetProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found", "USER_004"));
      return appointmentRepository.findByVeterinarian(vet, pageable).map(AppointmentResponse::fromEntity);
    } else {
      return appointmentRepository.findAll(pageable).map(AppointmentResponse::fromEntity);
    }
  }

  /** Retrieves a specific appointment by ID. */
  @Transactional(readOnly = true)
  @Cacheable(
      value = CacheNames.APPOINTMENTS,
      key = "T(app.vetra.infrastructure.cache.CacheKeys).appointmentKey(#id)")
  public AppointmentResponse getAppointmentById(String currentUserIdentifier, UUID id) {
    User user = getUserByEmail(currentUserIdentifier);
    Appointment appointment = appointmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id, "APPT_001"));

    validateUserAccess(user, appointment);
    return AppointmentResponse.fromEntity(appointment);
  }

  /** Updates appointment status according to centralized state machine rules. */
  @Transactional
  @Caching(
      evict = {
          @CacheEvict(
              value = CacheNames.APPOINTMENTS,
              key = "T(app.vetra.infrastructure.cache.CacheKeys).appointmentKey(#id)"),
          @CacheEvict(
              value = {CacheNames.DASHBOARD_FARMER, CacheNames.DASHBOARD_VET},
              allEntries = true)
      })
  public AppointmentResponse updateStatus(
      String currentUserIdentifier, UUID id, UpdateAppointmentStatusRequest request) {
    User user = getUserByEmail(currentUserIdentifier);
    Appointment appointment = appointmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id, "APPT_001"));

    validateUserAccess(user, appointment);
    applyStateTransition(user, appointment, request.status(), request.notes(), request.cancellationReason());

    Appointment updated = appointmentRepository.save(appointment);
    return AppointmentResponse.fromEntity(updated);
  }

  /** Delegate helper for confirming an appointment (Vet only). */
  @Transactional
  public AppointmentResponse confirmAppointment(String currentUserIdentifier, UUID id) {
    return updateStatus(currentUserIdentifier, id,
        new UpdateAppointmentStatusRequest(AppointmentStatus.CONFIRMED, null, null));
  }

  /** Delegate helper for rejecting an appointment (Vet only). */
  @Transactional
  public AppointmentResponse rejectAppointment(String currentUserIdentifier, UUID id, String reason) {
    return updateStatus(currentUserIdentifier, id,
        new UpdateAppointmentStatusRequest(AppointmentStatus.REJECTED, null, reason));
  }

  /** Delegate helper for completing an appointment (Vet only). */
  @Transactional
  public AppointmentResponse completeAppointment(String currentUserIdentifier, UUID id, String notes) {
    return updateStatus(currentUserIdentifier, id,
        new UpdateAppointmentStatusRequest(AppointmentStatus.COMPLETED, notes, null));
  }

  /** Delegate helper for cancelling an appointment (Farmer). */
  @Transactional
  public AppointmentResponse cancelAppointment(String currentUserIdentifier, UUID id, String reason) {
    return updateStatus(currentUserIdentifier, id,
        new UpdateAppointmentStatusRequest(AppointmentStatus.CANCELLED, null, reason));
  }

  /** Centralized state machine transition logic. */
  private void applyStateTransition(
      User user, Appointment appointment, AppointmentStatus targetStatus, String notes, String cancellationReason) {
    AppointmentStatus currentStatus = appointment.getStatus();

    if (currentStatus.isTerminal()) {
      throw new BusinessRuleException("Terminal appointments (COMPLETED, CANCELLED, REJECTED) cannot be edited", "APPT_005");
    }

    validateAllowedTransition(currentStatus, targetStatus);
    validateRolePermissions(user, targetStatus);

    appointment.setStatus(targetStatus);
    if (notes != null && !notes.isBlank()) {
      appointment.setVeterinarianNotes(notes);
    }
    if (cancellationReason != null && !cancellationReason.isBlank()) {
      appointment.setCancellationReason(cancellationReason);
    }

    recordStatusTransition(targetStatus);
  }

  private void recordStatusTransition(AppointmentStatus status) {
    switch (status) {
      case CONFIRMED -> vetraMetrics.recordAppointmentConfirmed();
      case COMPLETED -> vetraMetrics.recordAppointmentCompleted();
      case CANCELLED -> vetraMetrics.recordAppointmentCancelled();
      case REJECTED -> vetraMetrics.recordAppointmentRejected();
      default -> { /* PENDING not a transition target */ }
    }
  }

  private void validateAllowedTransition(AppointmentStatus current, AppointmentStatus target) {
    if (current == AppointmentStatus.PENDING) {
      if (target != AppointmentStatus.CONFIRMED && target != AppointmentStatus.REJECTED && target != AppointmentStatus.CANCELLED) {
        throw new BusinessRuleException("Invalid state transition from PENDING to " + target, "APPT_004");
      }
    } else if (current == AppointmentStatus.CONFIRMED) {
      if (target != AppointmentStatus.COMPLETED && target != AppointmentStatus.CANCELLED) {
        throw new BusinessRuleException("Invalid state transition from CONFIRMED to " + target, "APPT_004");
      }
    }
  }

  private void validateRolePermissions(User user, AppointmentStatus targetStatus) {
    boolean isVetAction = targetStatus == AppointmentStatus.CONFIRMED
        || targetStatus == AppointmentStatus.REJECTED
        || targetStatus == AppointmentStatus.COMPLETED;

    if (isVetAction && user.getRole() != UserRole.VETERINARIAN) {
      throw new UnauthorizedResourceAccessException("Only veterinarians can " + targetStatus.name().toLowerCase() + " appointments", "APPT_003");
    }

    if (targetStatus == AppointmentStatus.CANCELLED && user.getRole() != UserRole.FARMER) {
      throw new UnauthorizedResourceAccessException("Only requesting farmers can cancel appointments", "APPT_002");
    }
  }

  private void validateUserAccess(User user, Appointment appointment) {
    if (user.getRole() == UserRole.FARMER) {
      FarmerProfile farmer = farmerProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found", "USER_004"));
      if (!appointment.getFarmer().getId().equals(farmer.getId())) {
        throw new UnauthorizedResourceAccessException("Unauthorized access to farmer appointment", "APPT_002");
      }
    } else if (user.getRole() == UserRole.VETERINARIAN) {
      VetProfile vet = vetProfileRepository.findByUser(user)
          .orElseThrow(() -> new ResourceNotFoundException("Vet profile not found", "USER_004"));
      if (!appointment.getVeterinarian().getId().equals(vet.getId())) {
        throw new UnauthorizedResourceAccessException("Unauthorized access to veterinarian appointment", "APPT_003");
      }
    }
  }

  private User getUserByEmail(String email) {
    return userRepository.findByIdentifier(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email, "USER_004"));
  }
}
