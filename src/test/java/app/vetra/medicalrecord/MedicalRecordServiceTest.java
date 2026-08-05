package app.vetra.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.FarmerProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.ConflictException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.FarmerProfile;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.entity.VetProfile;
import app.vetra.infrastructure.persistence.enums.AppointmentStatus;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.medicalrecord.dto.CreateMedicalRecordRequest;
import app.vetra.medicalrecord.dto.MedicalRecordResponse;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import app.vetra.medicalrecord.service.MedicalRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

  @Mock private MedicalRecordRepository medicalRecordRepository;
  @Mock private AppointmentRepository appointmentRepository;
  @Mock private AnimalRepository animalRepository;
  @Mock private UserRepository userRepository;
  @Mock private FarmerProfileRepository farmerProfileRepository;
  @Mock private VetProfileRepository vetProfileRepository;

  @InjectMocks private MedicalRecordService medicalRecordService;

  private User vetUser;
  private User farmerUser;
  private VetProfile vetProfile;
  private FarmerProfile farmerProfile;
  private Animal animal;
  private Appointment appointment;
  private UUID appointmentId;
  private UUID vetProfileId;

  @BeforeEach
  void setUp() {
    appointmentId = UUID.randomUUID();
    vetProfileId = UUID.randomUUID();

    vetUser = new User();
    vetUser.setId(UUID.randomUUID());
    vetUser.setEmail("dr.smith@vetra.com");
    vetUser.setRole(UserRole.VETERINARIAN);
    vetUser.setActive(true);

    farmerUser = new User();
    farmerUser.setId(UUID.randomUUID());
    farmerUser.setEmail("farmer.john@vetra.com");
    farmerUser.setRole(UserRole.FARMER);
    farmerUser.setActive(true);

    vetProfile = new VetProfile();
    vetProfile.setId(vetProfileId);
    vetProfile.setUser(vetUser);
    vetProfile.setFullName("Dr. Smith");
    vetProfile.setClinicName("City Vet");

    farmerProfile = new FarmerProfile();
    farmerProfile.setId(UUID.randomUUID());
    farmerProfile.setUser(farmerUser);
    farmerProfile.setFullName("Farmer John");

    animal = new Animal();
    animal.setId(UUID.randomUUID());
    animal.setAnimalName("Bessie");
    animal.setTagNumber("TAG-999");
    animal.setSpecies(Species.CATTLE);
    animal.setFarmer(farmerProfile);

    appointment = new Appointment();
    appointment.setId(appointmentId);
    appointment.setFarmer(farmerProfile);
    appointment.setVeterinarian(vetProfile);
    appointment.setAnimal(animal);
    appointment.setStatus(AppointmentStatus.COMPLETED);
  }

  @Test
  @DisplayName("Should successfully create medical record for COMPLETED appointment")
  void createMedicalRecord_success() {
    CreateMedicalRecordRequest request =
        new CreateMedicalRecordRequest(
            appointmentId,
            "Bovine Mastitis",
            "Swelling",
            "Antibiotics IV",
            "Penicillin 500mg",
            new BigDecimal("450.00"),
            new BigDecimal("39.5"),
            LocalDate.now().plusDays(7),
            "Rest and monitor");

    when(userRepository.findByEmail("dr.smith@vetra.com")).thenReturn(Optional.of(vetUser));
    when(vetProfileRepository.findByUserId(vetUser.getId())).thenReturn(Optional.of(vetProfile));
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
    when(medicalRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(false);

    MedicalRecord saved =
        MedicalRecord.builder()
            .id(UUID.randomUUID())
            .appointment(appointment)
            .animal(animal)
            .farmer(farmerProfile)
            .veterinarian(vetProfile)
            .diagnosis(request.diagnosis())
            .treatment(request.treatment())
            .build();

    when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(saved);

    MedicalRecordResponse response =
        medicalRecordService.createMedicalRecord("dr.smith@vetra.com", request);

    assertNotNull(response);
    assertEquals("Bovine Mastitis", response.diagnosis());
    assertEquals("Dr. Smith", response.veterinarianName());
    verify(medicalRecordRepository).save(any(MedicalRecord.class));
  }

  @Test
  @DisplayName("Should reject medical record creation when appointment is NOT completed")
  void createMedicalRecord_fails_whenNotCompleted() {
    appointment.setStatus(AppointmentStatus.CONFIRMED);
    CreateMedicalRecordRequest request =
        new CreateMedicalRecordRequest(
            appointmentId, "Diagnosis", "Symptoms", "Treatment", null, null, null, null, null);

    when(userRepository.findByEmail("dr.smith@vetra.com")).thenReturn(Optional.of(vetUser));
    when(vetProfileRepository.findByUserId(vetUser.getId())).thenReturn(Optional.of(vetProfile));
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

    BusinessRuleException ex =
        assertThrows(
            BusinessRuleException.class,
            () -> medicalRecordService.createMedicalRecord("dr.smith@vetra.com", request));
    assertEquals("APPT_008", ex.getErrorCode());
  }

  @Test
  @DisplayName("Should reject duplicate medical record for same appointment")
  void createMedicalRecord_fails_whenDuplicateExists() {
    CreateMedicalRecordRequest request =
        new CreateMedicalRecordRequest(
            appointmentId, "Diagnosis", "Symptoms", "Treatment", null, null, null, null, null);

    when(userRepository.findByEmail("dr.smith@vetra.com")).thenReturn(Optional.of(vetUser));
    when(vetProfileRepository.findByUserId(vetUser.getId())).thenReturn(Optional.of(vetProfile));
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
    when(medicalRecordRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

    ConflictException ex =
        assertThrows(
            ConflictException.class,
            () -> medicalRecordService.createMedicalRecord("dr.smith@vetra.com", request));
    assertEquals("MEDICAL_004", ex.getErrorCode());
  }

  @Test
  @DisplayName("Should reject medical record creation by unauthorized veterinarian")
  void createMedicalRecord_fails_whenUnassignedVet() {
    VetProfile otherVet = new VetProfile();
    otherVet.setId(UUID.randomUUID());
    otherVet.setUser(vetUser);
    otherVet.setFullName("Other Vet");

    when(userRepository.findByEmail("dr.smith@vetra.com")).thenReturn(Optional.of(vetUser));
    when(vetProfileRepository.findByUserId(vetUser.getId())).thenReturn(Optional.of(otherVet));
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

    CreateMedicalRecordRequest request =
        new CreateMedicalRecordRequest(
            appointmentId, "Diagnosis", "Symptoms", "Treatment", null, null, null, null, null);

    UnauthorizedResourceAccessException ex =
        assertThrows(
            UnauthorizedResourceAccessException.class,
            () -> medicalRecordService.createMedicalRecord("dr.smith@vetra.com", request));
    assertEquals("MEDICAL_003", ex.getErrorCode());
  }

  @Test
  @DisplayName("Should reject farmer accessing medical history of unowned animal")
  void getAnimalMedicalHistory_fails_whenFarmerDoesNotOwnAnimal() {
    FarmerProfile otherFarmer = new FarmerProfile();
    otherFarmer.setId(UUID.randomUUID());
    otherFarmer.setUser(farmerUser);

    when(userRepository.findByEmail("farmer.john@vetra.com")).thenReturn(Optional.of(farmerUser));
    when(animalRepository.findById(animal.getId())).thenReturn(Optional.of(animal));
    when(farmerProfileRepository.findByUserId(farmerUser.getId()))
        .thenReturn(Optional.of(otherFarmer));

    UnauthorizedResourceAccessException ex =
        assertThrows(
            UnauthorizedResourceAccessException.class,
            () ->
                medicalRecordService.getAnimalMedicalHistory(
                    "farmer.john@vetra.com", animal.getId()));
    assertEquals("MEDICAL_002", ex.getErrorCode());
  }
}
