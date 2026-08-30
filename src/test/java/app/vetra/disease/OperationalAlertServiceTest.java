package app.vetra.disease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.vetra.disease.dto.OperationalAlertResponse;
import app.vetra.disease.entity.DiagnosisConfidenceSource;
import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.disease.service.OperationalAlertService;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationalAlertServiceTest {

  @Mock private OutbreakRepository outbreakRepository;
  @Mock private DiseaseReportRepository diseaseReportRepository;
  @Mock private VaccinationGapService vaccinationGapService;

  private OperationalAlertService alertService;

  @BeforeEach
  void setUp() {
    alertService =
        new OperationalAlertService(
            outbreakRepository, diseaseReportRepository, vaccinationGapService);
  }

  @Test
  void testListOperationalAlerts_EmptyOutbreaks_ReturnsEmptyList() {
    when(outbreakRepository.findAll()).thenReturn(List.of());

    List<OperationalAlertResponse> alerts = alertService.listOperationalAlerts();

    assertNotNull(alerts);
    assertTrue(alerts.isEmpty());
  }

  @Test
  void testListOperationalAlerts_CriticalOutbreak_GeneratesDeterministicAlerts() {
    UUID outbreakId = UUID.randomUUID();
    Outbreak outbreak =
        Outbreak.builder()
            .diseaseName("Rabies")
            .centerLatitude(19.0760)
            .centerLongitude(72.8777)
            .radiusKm(15.0)
            .status(OutbreakStatus.ACTIVE)
            .riskScore(OutbreakRiskScore.CRITICAL)
            .compositeRiskScore(88)
            .vaccinationGapScore(65.0)
            .affectedReportsCount(4)
            .build();
    outbreak.setId(outbreakId);
    outbreak.setCreatedAt(Instant.now().minusSeconds(3600));

    when(outbreakRepository.findAll()).thenReturn(List.of(outbreak));

    DiseaseReport report =
        DiseaseReport.builder()
            .diseaseName("Rabies")
            .diagnosisStatus(DiagnosisStatus.CONFIRMED)
            .diagnosisConfidenceSource(DiagnosisConfidenceSource.LAB_CONFIRMED)
            .latitude(19.0760)
            .longitude(72.8777)
            .build();

    when(diseaseReportRepository.findAll()).thenReturn(List.of(report));

    List<OperationalAlertResponse> alerts = alertService.listOperationalAlerts();

    assertNotNull(alerts);
    assertFalse(alerts.isEmpty());
    assertTrue(alerts.stream().anyMatch(a -> a.severity().equals("CRITICAL")));
    assertTrue(alerts.stream().anyMatch(a -> a.eventType().equals("CRITICAL_OUTBREAK_DETECTED")));
    assertTrue(alerts.stream().anyMatch(a -> a.eventType().equals("IMMUNITY_GAP_OVERLAP")));
    assertTrue(alerts.stream().anyMatch(a -> a.eventType().equals("CASE_VOLUME_ESCALATION")));
    assertTrue(alerts.stream().anyMatch(a -> a.eventType().equals("LAB_CONFIRMED_CLUSTER")));

    // Test getAlertById
    OperationalAlertResponse first = alerts.get(0);
    OperationalAlertResponse fetched = alertService.getAlertById(first.id());
    assertEquals(first.id(), fetched.id());
    assertEquals(first.diseaseName(), fetched.diseaseName());
  }

  @Test
  void testListOperationalAlerts_IdempotentEvaluation_NoDuplicateIds() {
    UUID outbreakId = UUID.randomUUID();
    Outbreak outbreak =
        Outbreak.builder()
            .diseaseName("Anthrax")
            .centerLatitude(18.5204)
            .centerLongitude(73.8567)
            .radiusKm(20.0)
            .status(OutbreakStatus.ACTIVE)
            .riskScore(OutbreakRiskScore.CRITICAL)
            .compositeRiskScore(92)
            .vaccinationGapScore(70.0)
            .affectedReportsCount(5)
            .build();
    outbreak.setId(outbreakId);
    outbreak.setCreatedAt(Instant.now().minusSeconds(7200));

    when(outbreakRepository.findAll()).thenReturn(List.of(outbreak));
    when(diseaseReportRepository.findAll()).thenReturn(List.of());

    // Call 1
    List<OperationalAlertResponse> run1 = alertService.listOperationalAlerts();
    // Call 2
    List<OperationalAlertResponse> run2 = alertService.listOperationalAlerts();

    assertEquals(run1.size(), run2.size());
    for (int i = 0; i < run1.size(); i++) {
      assertEquals(run1.get(i).id(), run2.get(i).id());
      assertEquals(run1.get(i).eventType(), run2.get(i).eventType());
    }

    // Verify zero duplicate IDs within a single run
    Set<UUID> uniqueIds = new HashSet<>();
    for (OperationalAlertResponse alert : run1) {
      assertTrue(uniqueIds.add(alert.id()), "Duplicate alert ID detected: " + alert.id());
    }
  }

  @Test
  void testGetAlertById_NotFound_ThrowsException() {
    when(outbreakRepository.findAll()).thenReturn(List.of());

    assertThrows(
        ResourceNotFoundException.class, () -> alertService.getAlertById(UUID.randomUUID()));
  }
}
