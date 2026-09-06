package app.vetra.vaccination.entity;

import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable audit log capturing government officer actions and status transitions
 * on vaccination campaigns.
 */
@Entity
@Table(name = "vaccination_campaign_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationCampaignAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private VaccinationCampaign campaign;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performed_by_id", nullable = false)
  private User performedBy;

  @Column(name = "action", nullable = false, length = 50)
  private String action;

  @Column(name = "previous_status", length = 30)
  private String previousStatus;

  @Column(name = "new_status", length = 30)
  private String newStatus;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
