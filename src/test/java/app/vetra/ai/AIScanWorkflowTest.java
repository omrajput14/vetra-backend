package app.vetra.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.dto.AIScanResponse;
import app.vetra.ai.dto.ApproveAIScanRequest;
import app.vetra.ai.dto.CreateAIScanRequest;
import app.vetra.ai.dto.RejectAIScanRequest;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.service.AIScanService;
import app.vetra.animal.dto.AnimalResponse;
import app.vetra.animal.dto.CreateAnimalRequest;
import app.vetra.animal.service.AnimalService;
import app.vetra.auth.dto.FarmerRegisterRequest;
import app.vetra.auth.dto.VetRegisterRequest;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.exception.BusinessRuleException;
import app.vetra.infrastructure.exception.UnauthorizedResourceAccessException;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.enums.AnimalGender;
import app.vetra.infrastructure.persistence.enums.Species;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end integration test suite verifying Stage 9.4 AI Scan Review Workflow,
 * veterinarian approval/rejection lifecycle, state transition validations,
 * and automated Electronic Veterinary Medical Record (EVMR) creation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:vetra_workflow_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "vetra.jwt.secret=test-jwt-secret-value-minimum-32-characters-long",
    "vetra.jwt.expiration-ms=86400000",
    "vetra.jwt.refresh-expiration-ms=604800000",
    "vetra.cors.allowed-origins=http://localhost:3000",
    "vetra.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
    "vetra.cors.allowed-headers=*",
    "vetra.cors.allow-credentials=true",
    "vetra.cors.max-age=3600",
    "vetra.aws.region=ap-south-1",
    "vetra.aws.credentials.access-key=test-key",
    "vetra.aws.credentials.secret-key=test-secret",
    "vetra.aws.s3.bucket-name=vetra-test-bucket",
    "vetra.aws.s3.presigned-url-expiry-minutes=15",
    "vetra.ai.enabled=false",
    "vetra.ai.default-provider=NONE",
    "vetra.ai.retry.max-attempts=1",
    "vetra.ai.retry.backoff=1ms"
})
class AIScanWorkflowTest {

  @Autowired private AIScanService aiScanService;
  @Autowired private AnimalService animalService;
  @Autowired private AuthService authService;
  @Autowired private MedicalRecordRepository medicalRecordRepository;

  @Test
  void testSuccessfulApprovalAndAutomaticMedicalRecordCreation() {
    // 1. Setup Users & Animal
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_wf@vetra.app", "+1555999001", "pass123", "Farmer Joe", "Green Pastures",
        "Village", "District", "State", 12.0, 56.0, 15));

    authService.registerVet(new VetRegisterRequest(
        "vet_wf@vetra.app", "+1555999002", "pass123", "Dr. Sarah", "VET-REG-8811",
        "BVSc", "Epidemiology", "City Vet Clinic", 8, 12.1, 56.1));

    AnimalResponse animal = animalService.createAnimal("farmer_wf@vetra.app",
        new CreateAnimalRequest("Bessie", "TAG-WF-1", "QR-WF-1", Species.CATTLE, "Jersey", AnimalGender.FEMALE, LocalDate.now().minusYears(3), null));

    // 2. Submit AI Scan
    AIScanResponse createdScan = aiScanService.createScan("farmer_wf@vetra.app",
        new CreateAIScanRequest(animal.id(), "https://s3.amazonaws.com/vetra/scans/bessie.jpg", "HASH-112233"));

    // Set status to COMPLETED for test review
    aiScanService.updateStatus(createdScan.id(), AIScanStatus.COMPLETED, "Suspected Bovine Dermatitis", new BigDecimal("0.890"));

    // 3. Veterinarian Approves Scan
    ApproveAIScanRequest approveReq = new ApproveAIScanRequest(
        "Confirmed dermatitis observation.", "Bovine Dermatitis (Confirmed)", "Topical antiseptic spray twice daily.");

    AIScanResponse approvedScan = aiScanService.approveScan("vet_wf@vetra.app", createdScan.id(), approveReq);

    assertEquals(AIScanStatus.VERIFIED, approvedScan.status());
    assertTrue(approvedScan.veterinarianVerified());
    assertEquals("Bovine Dermatitis (Confirmed)", approvedScan.diagnosis());
    assertEquals("Confirmed dermatitis observation.", approvedScan.notes());

    // 4. Verify Automatic Immutable MedicalRecord Creation
    List<MedicalRecord> records = medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(animal.id());
    assertEquals(1, records.size());

    MedicalRecord record = records.get(0);
    assertEquals("Bovine Dermatitis (Confirmed)", record.getDiagnosis());
    assertEquals("Topical antiseptic spray twice daily.", record.getTreatment());
    assertNotNull(record.getCreatedAt());
  }

  @Test
  void testSuccessfulRejectionFlow() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_rej@vetra.app", "+1555999003", "pass123", "Farmer Dan", "Valley Farm",
        "Village", "District", "State", 12.0, 56.0, 5));

    authService.registerVet(new VetRegisterRequest(
        "vet_rej@vetra.app", "+1555999004", "pass123", "Dr. Marcus", "VET-REG-9922",
        "BVSc", "Surgeon", "Central Vet", 10, 12.1, 56.1));

    AnimalResponse animal = animalService.createAnimal("farmer_rej@vetra.app",
        new CreateAnimalRequest("Molly", "TAG-WF-2", "QR-WF-2", Species.GOAT, "Boer", AnimalGender.FEMALE, LocalDate.now().minusYears(1), null));

    AIScanResponse createdScan = aiScanService.createScan("farmer_rej@vetra.app",
        new CreateAIScanRequest(animal.id(), "https://s3.amazonaws.com/vetra/scans/molly.jpg", "HASH-445566"));

    aiScanService.updateStatus(createdScan.id(), AIScanStatus.COMPLETED, "False Positive AI Artifact", new BigDecimal("0.450"));

    // Rejection by Vet
    AIScanResponse rejectedScan = aiScanService.rejectScan("vet_rej@vetra.app", createdScan.id(),
        new RejectAIScanRequest("Image quality blurry and diagnostic artifact is false positive."));

    assertEquals(AIScanStatus.REJECTED, rejectedScan.status());
    assertTrue(rejectedScan.veterinarianVerified());
    assertTrue(rejectedScan.notes().contains("false positive"));

    // MedicalRecord MUST NOT be created on rejection
    List<MedicalRecord> records = medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(animal.id());
    assertTrue(records.isEmpty());
  }

  @Test
  void testUnauthorizedFarmerAttemptToApproveRejectsWith403() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_unauth@vetra.app", "+1555999005", "pass123", "Farmer Alex", "Sunrise Farm",
        "Village", "District", "State", 12.0, 56.0, 8));

    AnimalResponse animal = animalService.createAnimal("farmer_unauth@vetra.app",
        new CreateAnimalRequest("Bella", "TAG-WF-3", "QR-WF-3", Species.SHEEP, "Merino", AnimalGender.FEMALE, LocalDate.now().minusYears(2), null));

    AIScanResponse scan = aiScanService.createScan("farmer_unauth@vetra.app",
        new CreateAIScanRequest(animal.id(), "https://s3.amazonaws.com/vetra/scans/bella.jpg", "HASH-778899"));

    aiScanService.updateStatus(scan.id(), AIScanStatus.COMPLETED, "Mild Observation", new BigDecimal("0.750"));

    assertThrows(UnauthorizedResourceAccessException.class, () ->
        aiScanService.approveScan("farmer_unauth@vetra.app", scan.id(), new ApproveAIScanRequest("Farmer self-approval", null, null)));
  }

  @Test
  void testDoubleApprovalAttemptRejectsWith422() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_double@vetra.app", "+1555999007", "pass123", "Farmer Tim", "Highland Farm",
        "Village", "District", "State", 12.0, 56.0, 12));

    authService.registerVet(new VetRegisterRequest(
        "vet_double@vetra.app", "+1555999008", "pass123", "Dr. Emma", "VET-REG-3344",
        "BVSc", "Clinician", "North Clinic", 5, 12.1, 56.1));

    AnimalResponse animal = animalService.createAnimal("farmer_double@vetra.app",
        new CreateAnimalRequest("Luna", "TAG-WF-4", "QR-WF-4", Species.CATTLE, "Angus", AnimalGender.FEMALE, LocalDate.now().minusYears(4), null));

    AIScanResponse scan = aiScanService.createScan("farmer_double@vetra.app",
        new CreateAIScanRequest(animal.id(), "https://s3.amazonaws.com/vetra/scans/luna.jpg", "HASH-001122"));

    aiScanService.updateStatus(scan.id(), AIScanStatus.COMPLETED, "Initial Diagnosis", new BigDecimal("0.910"));

    // First Approval -> Success
    aiScanService.approveScan("vet_double@vetra.app", scan.id(), new ApproveAIScanRequest("Approved", null, null));

    // Second Approval Attempt -> Throws BusinessRuleException (422)
    assertThrows(BusinessRuleException.class, () ->
        aiScanService.approveScan("vet_double@vetra.app", scan.id(), new ApproveAIScanRequest("Duplicate approval", null, null)));
  }

  @Test
  void testReviewOfFailedScanRejectsWith422() {
    authService.registerFarmer(new FarmerRegisterRequest(
        "farmer_fail@vetra.app", "+1555999009", "pass123", "Farmer Carl", "West Farm",
        "Village", "District", "State", 12.0, 56.0, 4));

    authService.registerVet(new VetRegisterRequest(
        "vet_fail@vetra.app", "+1555999010", "pass123", "Dr. Oliver", "VET-REG-5566",
        "BVSc", "Clinician", "South Clinic", 7, 12.1, 56.1));

    AnimalResponse animal = animalService.createAnimal("farmer_fail@vetra.app",
        new CreateAnimalRequest("Max", "TAG-WF-5", "QR-WF-5", Species.CATTLE, "Hereford", AnimalGender.MALE, LocalDate.now().minusYears(1), null));

    AIScanResponse scan = aiScanService.createScan("farmer_fail@vetra.app",
        new CreateAIScanRequest(animal.id(), "https://s3.amazonaws.com/vetra/scans/max.jpg", "HASH-334455"));

    aiScanService.updateStatus(scan.id(), AIScanStatus.FAILED, null, null);

    assertThrows(BusinessRuleException.class, () ->
        aiScanService.approveScan("vet_fail@vetra.app", scan.id(), new ApproveAIScanRequest("Approval of failed scan", null, null)));
  }
}
