package app.vetra.ai.entity;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an AI livestock diagnostic image scan and verification audit trail.
 */
@Entity
@Table(name = "ai_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIScan extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "uploaded_by", nullable = false)
  private User uploadedBy;

  @Column(name = "image_url", nullable = false, length = 512)
  private String imageUrl;

  @Column(name = "image_hash", length = 64)
  private String imageHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "ai_provider", nullable = false, length = 32)
  @Builder.Default
  private AIProviderType aiProvider = AIProviderType.NONE;

  @Column(name = "ai_model", length = 64)
  private String aiModel;

  @Column(name = "diagnosis", columnDefinition = "TEXT")
  private String diagnosis;

  @Column(name = "confidence_score", precision = 4, scale = 3)
  private BigDecimal confidenceScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @Builder.Default
  private AIScanStatus status = AIScanStatus.PENDING;

  @Column(name = "veterinarian_verified", nullable = false)
  @Builder.Default
  private boolean veterinarianVerified = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by")
  private User verifiedBy;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Version
  @Column(name = "version", nullable = false)
  @Builder.Default
  private Long version = 0L;
}
