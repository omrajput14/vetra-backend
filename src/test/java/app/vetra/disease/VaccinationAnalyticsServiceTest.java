package app.vetra.disease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.disease.dto.VaccinationAnalyticsResponse;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.entity.OutbreakRiskScore;
import app.vetra.disease.entity.OutbreakStatus;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.disease.risk.vaccination.VaccinationGapService;
import app.vetra.disease.risk.vaccination.VaccinationGapService.VaccinationGapResult;
import app.vetra.disease.service.VaccinationAnalyticsService;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.AnimalHealthRecord;
import app.vetra.infrastructure.persistence.enums.HealthRecordType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationAnalyticsServiceTest {

  @Mock private AnimalRepository animalRepository;
  @Mock private AnimalHealthRecordRepository healthRecordRepository;
  @Mock private OutbreakRepository outbreakRepository;
  @Mock private VaccinationGapService vaccinationGapService;

  private VaccinationAnalyticsService analyticsService;

  @BeforeEach
  void setUp() {
    analyticsService =
        new VaccinationAnalyticsService(
            animalRepository, healthRecordRepository, outbreakRepository, vaccinationGapService);
  }

  @Test
  void testGetVaccinationAnalytics_EmptyAnimals_ReturnsZeroBaseline() {
    when(animalRepository.findAll()).thenReturn(List.of());

    VaccinationAnalyticsResponse response = analyticsService.getVaccinationAnalytics();

    assertNotNull(response);
    assertEquals(0, response.totalRegisteredLivestock());
    assertEquals(0, response.totalVaccinatedLivestock());
    assertEquals(0.0, response.overallCoveragePercentage());
    assertEquals(100.0, response.overallImmunityGapPercentage());
    assertTrue(response.pathogenCoverage().isEmpty());
  }

  @Test
  void testGetVaccinationAnalytics_WithAnimalsAndOutbreak_CalculatesRealCoverage() {
    Animal animal1 = new Animal();
    animal1.setId(UUID.randomUUID());
    Animal animal2 = new Animal();
    animal2.setId(UUID.randomUUID());

    when(animalRepository.findAll()).thenReturn(List.of(animal1, animal2));

    AnimalHealthRecord record = new AnimalHealthRecord();
    record.setId(UUID.randomUUID());
    record.setAnimal(animal1);
    record.setRecordType(HealthRecordType.VACCINATION);
    record.setVaccineName("FMD Biovax");
    record.setTitle("FMD Vaccine Dose");
    record.setRecordedAt(LocalDateTime.now().minusDays(30));

    when(healthRecordRepository.findAll()).thenReturn(List.of(record));

    Outbreak outbreak =
        Outbreak.builder()
            .diseaseName("Foot and Mouth Disease (FMD)")
            .centerLatitude(18.5204)
            .centerLongitude(73.8567)
            .radiusKm(10.0)
            .status(OutbreakStatus.ACTIVE)
            .riskScore(OutbreakRiskScore.CRITICAL)
            .compositeRiskScore(85)
            .vaccinationGapScore(50.0)
            .affectedReportsCount(5)
            .build();
    outbreak.setId(UUID.randomUUID());

    when(outbreakRepository.findAll()).thenReturn(List.of(outbreak));
    when(vaccinationGapService.calculateVaccinationGap(anyString(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(new VaccinationGapResult(50.0, 2, 1, 50.0, 50.0, "50% coverage"));

    VaccinationAnalyticsResponse response = analyticsService.getVaccinationAnalytics();

    assertNotNull(response);
    assertEquals(2, response.totalRegisteredLivestock());
    assertEquals(1, response.totalVaccinatedLivestock());
    assertEquals(1, response.totalUnvaccinatedLivestock());
    assertEquals(50.0, response.overallCoveragePercentage());
    assertEquals(50.0, response.overallImmunityGapPercentage());
    assertEquals(7, response.pathogenCoverage().size());
    assertEquals(1, response.zoneVaccinationGaps().size());
    assertEquals(1, response.priorityDeficitZones().size());
  }
}
