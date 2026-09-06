package app.vetra.vaccination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.vetra.auth.repository.UserRepository;
import app.vetra.disease.entity.Outbreak;
import app.vetra.disease.repository.OutbreakRepository;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.vaccination.dto.CreateVaccinationCampaignRequest;
import app.vetra.vaccination.dto.UpdateCampaignStatusRequest;
import app.vetra.vaccination.entity.VaccinationCampaign;
import app.vetra.vaccination.entity.VaccinationCampaignAuditLog;
import app.vetra.vaccination.enums.CampaignPriority;
import app.vetra.vaccination.enums.CampaignStatus;
import app.vetra.vaccination.repository.VaccinationCampaignAuditLogRepository;
import app.vetra.vaccination.repository.VaccinationCampaignRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Phase 4A: Government Response + Vaccination Campaign Management.
 *
 * <p>Validates strict RBAC, disease taxonomy validation, administrative district validation,
 * planned doses validation, server-side lifecycle state machine, idempotency replay, and audit logs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vetra_campaign_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
      "vetra.ai.gateway.provider=noop",
      "vetra.ai.gemini.enabled=false",
      "vetra.firebase.enabled=false",
      "vetra.openmeteo.enabled=false"
    })
class VaccinationCampaignIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private VaccinationCampaignRepository campaignRepository;
  @Autowired private VaccinationCampaignAuditLogRepository auditLogRepository;
  @Autowired private OutbreakRepository outbreakRepository;
  @Autowired private ObjectMapper objectMapper;

  private User governmentOfficer;
  private User administrator;
  private User farmer;
  private User veterinarian;

  @BeforeEach
  void setUp() {
    governmentOfficer = userRepository.save(User.builder()
        .email("officer@gov.vetra.in")
        .phone("+919111111111")
        .passwordHash("hashed")
        .role(UserRole.GOVERNMENT_OFFICER)
        .isActive(true)
        .build());

    administrator = userRepository.save(User.builder()
        .email("admin@vetra.in")
        .phone("+919222222222")
        .passwordHash("hashed")
        .role(UserRole.ADMINISTRATOR)
        .isActive(true)
        .build());

    farmer = userRepository.save(User.builder()
        .email("farmer@farm.in")
        .phone("+919333333333")
        .passwordHash("hashed")
        .role(UserRole.FARMER)
        .isActive(true)
        .build());

    veterinarian = userRepository.save(User.builder()
        .email("vet@clinic.in")
        .phone("+919444444444")
        .passwordHash("hashed")
        .role(UserRole.VETERINARIAN)
        .isActive(true)
        .build());
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("GOVERNMENT_OFFICER can launch real vaccination campaign with administeredDoses=0")
  void governmentOfficerCanLaunchCampaign() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Baramati FMD Ring Containment 2026",
        "Foot and Mouth Disease",
        "Pune",
        "Baramati",
        500,
        1500,
        CampaignPriority.HIGH,
        LocalDate.now(),
        LocalDate.now().plusDays(14),
        null,
        "Emergency ring vaccination triggered due to active cluster."
    );

    MvcResult result = mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.campaignName").value("Baramati FMD Ring Containment 2026"))
        .andExpect(jsonPath("$.data.diseaseName").value("Foot and Mouth Disease"))
        .andExpect(jsonPath("$.data.targetDistrict").value("Pune"))
        .andExpect(jsonPath("$.data.targetTaluka").value("Baramati"))
        .andExpect(jsonPath("$.data.plannedDoses").value(1500))
        .andExpect(jsonPath("$.data.administeredDoses").value(0))
        .andExpect(jsonPath("$.data.coverageProgressPercentage").value(0.0))
        .andExpect(jsonPath("$.data.status").value("PLANNED"))
        .andExpect(jsonPath("$.data.priority").value("HIGH"))
        .andReturn();

    // Verify DB persistence
    List<VaccinationCampaign> all = campaignRepository.findAll();
    assertThat(all).hasSize(1);
    VaccinationCampaign campaign = all.get(0);
    assertThat(campaign.getCampaignName()).isEqualTo("Baramati FMD Ring Containment 2026");
    assertThat(campaign.getAdministeredDoses()).isEqualTo(0);
    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PLANNED);
    assertThat(campaign.getCreatedBy().getId()).isEqualTo(governmentOfficer.getId());

    // Verify audit log created
    List<VaccinationCampaignAuditLog> logs = auditLogRepository.findByCampaignIdOrderByCreatedAtDesc(campaign.getId());
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getAction()).isEqualTo("CREATED");
    assertThat(logs.get(0).getNewStatus()).isEqualTo("PLANNED");
    assertThat(logs.get(0).getPerformedBy().getId()).isEqualTo(governmentOfficer.getId());
  }

  @Test
  @WithMockUser(username = "admin@vetra.in", roles = "ADMINISTRATOR")
  @DisplayName("ADMINISTRATOR can also launch vaccination campaign")
  void administratorCanLaunchCampaign() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Satara Rabies Prevention Drive",
        "Rabies",
        "Satara",
        null,
        null,
        500,
        CampaignPriority.CRITICAL,
        LocalDate.now(),
        null,
        null,
        "Routine prevention drive"
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PLANNED"));
  }

  @Test
  @WithMockUser(username = "farmer@farm.in", roles = "FARMER")
  @DisplayName("FARMER is forbidden from launching campaign (403 Forbidden)")
  void farmerForbiddenFromLaunchingCampaign() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Unauthorized Campaign",
        "Foot and Mouth Disease",
        "Pune",
        null,
        null,
        100,
        CampaignPriority.MEDIUM,
        LocalDate.now(),
        null,
        null,
        "Unauthorized"
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "vet@clinic.in", roles = "VETERINARIAN")
  @DisplayName("VETERINARIAN is forbidden from launching campaign (403 Forbidden)")
  void veterinarianForbiddenFromLaunchingCampaign() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Unauthorized Vet Campaign",
        "Foot and Mouth Disease",
        "Pune",
        null,
        null,
        100,
        CampaignPriority.MEDIUM,
        LocalDate.now(),
        null,
        null,
        "Unauthorized"
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Unknown disease is rejected with 400 Bad Request")
  void unknownDiseaseRejected() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Fictional Disease Campaign",
        "AlienInfection",
        "Pune",
        null,
        null,
        1000,
        CampaignPriority.MEDIUM,
        LocalDate.now(),
        null,
        null,
        "Test unknown disease"
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Invalid district is rejected with 400 Bad Request")
  void invalidDistrictRejected() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Atlantis Campaign",
        "Foot and Mouth Disease",
        "Atlantis",
        null,
        null,
        1000,
        CampaignPriority.MEDIUM,
        LocalDate.now(),
        null,
        null,
        "Test invalid district"
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Planned doses <= 0 is rejected with 400 Bad Request")
  void nonPositivePlannedDosesRejected() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Zero Doses Campaign",
        "Foot and Mouth Disease",
        "Pune",
        null,
        null,
        0,
        CampaignPriority.MEDIUM,
        LocalDate.now(),
        null,
        null,
        "Zero doses"
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Valid status lifecycle: PLANNED -> ACTIVE -> COMPLETED is permitted")
  void validStatusLifecycleProgression() throws Exception {
    VaccinationCampaign campaign = campaignRepository.save(VaccinationCampaign.builder()
        .campaignName("Lifecycle Test Campaign")
        .diseaseName("Foot and Mouth Disease")
        .targetDistrict("Pune")
        .targetLivestockCount(500)
        .plannedDoses(1000)
        .administeredDoses(0)
        .priority(CampaignPriority.HIGH)
        .status(CampaignStatus.PLANNED)
        .startDate(LocalDate.now())
        .createdBy(governmentOfficer)
        .build());

    // 1. PLANNED -> ACTIVE
    mockMvc.perform(patch("/api/v1/vaccination/campaigns/" + campaign.getId() + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UpdateCampaignStatusRequest(CampaignStatus.ACTIVE, "Teams deployed"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    // 2. ACTIVE -> COMPLETED
    mockMvc.perform(patch("/api/v1/vaccination/campaigns/" + campaign.getId() + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UpdateCampaignStatusRequest(CampaignStatus.COMPLETED, "Ring coverage complete"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));

    // Verify DB
    VaccinationCampaign updated = campaignRepository.findById(campaign.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(CampaignStatus.COMPLETED);

    // Verify audit logs
    List<VaccinationCampaignAuditLog> logs = auditLogRepository.findByCampaignIdOrderByCreatedAtDesc(campaign.getId());
    assertThat(logs).hasSize(2);
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Invalid status transition (PLANNED -> COMPLETED) is rejected with 422 Unprocessable Entity")
  void invalidStatusTransitionRejected() throws Exception {
    VaccinationCampaign campaign = campaignRepository.save(VaccinationCampaign.builder()
        .campaignName("Invalid Transition Campaign")
        .diseaseName("Foot and Mouth Disease")
        .targetDistrict("Pune")
        .targetLivestockCount(500)
        .plannedDoses(1000)
        .administeredDoses(0)
        .priority(CampaignPriority.HIGH)
        .status(CampaignStatus.PLANNED)
        .startDate(LocalDate.now())
        .createdBy(governmentOfficer)
        .build());

    mockMvc.perform(patch("/api/v1/vaccination/campaigns/" + campaign.getId() + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UpdateCampaignStatusRequest(CampaignStatus.COMPLETED, "Premature completion"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.success").value(false));

    // Confirm DB status unchanged
    VaccinationCampaign unchanged = campaignRepository.findById(campaign.getId()).orElseThrow();
    assertThat(unchanged.getStatus()).isEqualTo(CampaignStatus.PLANNED);
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Idempotency key prevents duplicate campaign on retry")
  void idempotencyPreventsDuplicateCampaign() throws Exception {
    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Idempotent Ring Drive",
        "Foot and Mouth Disease",
        "Pune",
        "Baramati",
        400,
        1200,
        CampaignPriority.HIGH,
        LocalDate.now(),
        null,
        null,
        "Idempotent creation test"
    );

    String idempotencyKey = "idemp-ring-vac-" + UUID.randomUUID();

    // First attempt
    MvcResult result1 = mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    String campaignId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
        .path("data").path("id").asText();

    // Second retry with identical idempotency key
    MvcResult result2 = mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    String campaignId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
        .path("data").path("id").asText();

    // Both calls must return identical campaign ID and exactly 1 record in DB
    assertThat(campaignId1).isEqualTo(campaignId2);
    assertThat(campaignRepository.findAll()).hasSize(1);
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Campaign can link to an existing Outbreak cluster")
  void campaignCanLinkToOutbreak() throws Exception {
    Outbreak outbreak = outbreakRepository.save(Outbreak.builder()
        .diseaseName("Foot and Mouth Disease")
        .centerLatitude(18.15)
        .centerLongitude(74.58)
        .radiusKm(15.0)
        .affectedReportsCount(12)
        .build());

    CreateVaccinationCampaignRequest request = new CreateVaccinationCampaignRequest(
        "Ring Outbreak Containment",
        "Foot and Mouth Disease",
        "Pune",
        "Baramati",
        300,
        800,
        CampaignPriority.CRITICAL,
        LocalDate.now(),
        LocalDate.now().plusDays(7),
        outbreak.getId(),
        "Targeted ring vaccination for active outbreak cluster."
    );

    mockMvc.perform(post("/api/v1/vaccination/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.outbreakId").value(outbreak.getId().toString()));
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Audit logs endpoint returns full officer history for a campaign")
  void auditLogsEndpointReturnsHistory() throws Exception {
    VaccinationCampaign campaign = campaignRepository.save(VaccinationCampaign.builder()
        .campaignName("Audit Log Test Drive")
        .diseaseName("Rabies")
        .targetDistrict("Satara")
        .targetLivestockCount(200)
        .plannedDoses(500)
        .administeredDoses(0)
        .priority(CampaignPriority.HIGH)
        .status(CampaignStatus.PLANNED)
        .startDate(LocalDate.now())
        .createdBy(governmentOfficer)
        .build());

    auditLogRepository.save(VaccinationCampaignAuditLog.builder()
        .campaign(campaign)
        .performedBy(governmentOfficer)
        .action("CREATED")
        .newStatus("PLANNED")
        .notes("Initial creation")
        .build());

    auditLogRepository.save(VaccinationCampaignAuditLog.builder()
        .campaign(campaign)
        .performedBy(governmentOfficer)
        .action("STATUS_CHANGED")
        .previousStatus("PLANNED")
        .newStatus("ACTIVE")
        .notes("Field deployment")
        .build());

    mockMvc.perform(get("/api/v1/vaccination/campaigns/" + campaign.getId() + "/audit-logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].action").value("STATUS_CHANGED"))
        .andExpect(jsonPath("$.data[1].action").value("CREATED"));
  }

  @Test
  @WithMockUser(username = "officer@gov.vetra.in", roles = "GOVERNMENT_OFFICER")
  @DisplayName("Statistics endpoint computes real campaign counts and dose totals")
  void statisticsEndpointComputesRealMetrics() throws Exception {
    campaignRepository.save(VaccinationCampaign.builder()
        .campaignName("Planned Camp")
        .diseaseName("Foot and Mouth Disease")
        .targetDistrict("Pune")
        .plannedDoses(1000)
        .administeredDoses(0)
        .status(CampaignStatus.PLANNED)
        .startDate(LocalDate.now())
        .createdBy(governmentOfficer)
        .build());

    campaignRepository.save(VaccinationCampaign.builder()
        .campaignName("Active Camp")
        .diseaseName("Rabies")
        .targetDistrict("Satara")
        .plannedDoses(2000)
        .administeredDoses(500)
        .status(CampaignStatus.ACTIVE)
        .startDate(LocalDate.now())
        .createdBy(governmentOfficer)
        .build());

    mockMvc.perform(get("/api/v1/vaccination/campaigns/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalCampaigns").value(2))
        .andExpect(jsonPath("$.data.plannedCampaigns").value(1))
        .andExpect(jsonPath("$.data.activeCampaigns").value(1))
        .andExpect(jsonPath("$.data.totalPlannedDoses").value(3000))
        .andExpect(jsonPath("$.data.totalAdministeredDoses").value(500));
  }
}
