package app.vetra.disease.service;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.entity.AIScanStatus;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.auth.repository.ParaVetProfileRepository;
import app.vetra.auth.repository.VetProfileRepository;
import app.vetra.infrastructure.exception.ResourceNotFoundException;
import app.vetra.infrastructure.persistence.entity.ParaVetProfile;
import app.vetra.infrastructure.persistence.enums.VerificationStatus;
import app.vetra.notification.entity.NotificationChannel;
import app.vetra.notification.entity.NotificationPriority;
import app.vetra.notification.service.NotificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Field workforce for officers: vets and para-vets, what each has done, para-vet approval. */
// ponytail: counts activity in memory from all scans and records; move to GROUP BY queries at scale.
@Service
public class WorkforceService {

  private final VetProfileRepository vetProfileRepository;
  private final ParaVetProfileRepository paraVetProfileRepository;
  private final AIScanRepository aiScanRepository;
  private final AnimalHealthRecordRepository healthRecordRepository;
  private final NotificationService notificationService;

  public WorkforceService(
      VetProfileRepository vetProfileRepository,
      ParaVetProfileRepository paraVetProfileRepository,
      AIScanRepository aiScanRepository,
      AnimalHealthRecordRepository healthRecordRepository,
      NotificationService notificationService) {
    this.vetProfileRepository = vetProfileRepository;
    this.paraVetProfileRepository = paraVetProfileRepository;
    this.aiScanRepository = aiScanRepository;
    this.healthRecordRepository = healthRecordRepository;
    this.notificationService = notificationService;
  }

  /** One vet or para-vet with their activity. */
  public record Member(
      UUID userId, String name, String role, String district, String taluka, String status,
      long scansEscalated, long scansClosed, long casesConfirmed, long scansRejected, long dosesGiven) {}

  @Transactional(readOnly = true)
  public List<Member> list() {
    List<AIScan> scans = aiScanRepository.findAll();
    Map<UUID, Long> escalated = count(scans.stream().filter(s -> s.getTriagedBy() != null).toList(),
        s -> s.getTriagedBy().getId());
    Map<UUID, Long> confirmed = count(scans.stream()
        .filter(s -> s.getStatus() == AIScanStatus.VERIFIED && s.getVerifiedBy() != null).toList(),
        s -> s.getVerifiedBy().getId());
    Map<UUID, Long> rejected = count(scans.stream()
        .filter(s -> s.getStatus() == AIScanStatus.REJECTED && s.getVerifiedBy() != null).toList(),
        s -> s.getVerifiedBy().getId());
    Map<UUID, Long> doses = healthRecordRepository.findAll().stream()
        .filter(r -> r.getCampaignId() != null && r.getAdministeredBy() != null)
        .collect(Collectors.groupingBy(r -> r.getAdministeredBy(), Collectors.counting()));

    List<Member> out = new ArrayList<>();
    for (ParaVetProfile p : paraVetProfileRepository.findAll()) {
      UUID id = p.getUser().getId();
      out.add(new Member(id, p.getFullName(), "PARA_VET", p.getDistrict(), p.getTaluka(),
          p.getVerificationStatus().name(), escalated.getOrDefault(id, 0L), rejected.getOrDefault(id, 0L),
          0, 0, doses.getOrDefault(id, 0L)));
    }
    vetProfileRepository.findAll().forEach(v -> {
      UUID id = v.getUser().getId();
      out.add(new Member(id, v.getFullName(), "VETERINARIAN", v.getDistrict(), v.getTaluka(),
          v.getVerificationStatus() != null ? v.getVerificationStatus().name() : null, 0, 0,
          confirmed.getOrDefault(id, 0L), rejected.getOrDefault(id, 0L), doses.getOrDefault(id, 0L)));
    });
    return out;
  }

  /** Approve or reject a para-vet; they get a push either way. */
  @Transactional
  public Member setParaVetStatus(UUID userId, VerificationStatus status) {
    ParaVetProfile p = paraVetProfileRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Para-vet not found", "USER_004"));
    p.setVerificationStatus(status);
    paraVetProfileRepository.save(p);
    boolean ok = status == VerificationStatus.VERIFIED;
    notificationService.sendNotification(userId,
        ok ? "Para-vet account approved" : "Para-vet account not approved",
        ok ? "The district office approved your account. You can now check scans and record vaccination doses."
            : "The district office did not approve your para-vet account. Contact your district office.",
        "{\"route\":\"/paravet-home\"}", NotificationChannel.PUSH, NotificationPriority.HIGH);
    return list().stream().filter(m -> m.userId().equals(userId)).findFirst().orElseThrow();
  }

  private static Map<UUID, Long> count(List<AIScan> scans, Function<AIScan, UUID> key) {
    return scans.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
  }
}
