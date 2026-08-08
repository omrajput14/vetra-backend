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

/** Entity representing an AI livestock diagnostic image scan and verification audit trail. */
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

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public User getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(User uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getImageHash() {
    return imageHash;
  }

  public void setImageHash(String imageHash) {
    this.imageHash = imageHash;
  }

  public AIProviderType getAiProvider() {
    return aiProvider;
  }

  public void setAiProvider(AIProviderType aiProvider) {
    this.aiProvider = aiProvider;
  }

  public String getAiModel() {
    return aiModel;
  }

  public void setAiModel(String aiModel) {
    this.aiModel = aiModel;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public void setDiagnosis(String diagnosis) {
    this.diagnosis = diagnosis;
  }

  public BigDecimal getConfidenceScore() {
    return confidenceScore;
  }

  public void setConfidenceScore(BigDecimal confidenceScore) {
    this.confidenceScore = confidenceScore;
  }

  public AIScanStatus getStatus() {
    return status;
  }

  public void setStatus(AIScanStatus status) {
    this.status = status;
  }

  public boolean isVeterinarianVerified() {
    return veterinarianVerified;
  }

  public void setVeterinarianVerified(boolean veterinarianVerified) {
    this.veterinarianVerified = veterinarianVerified;
  }

  public User getVerifiedBy() {
    return verifiedBy;
  }

  public void setVerifiedBy(User verifiedBy) {
    this.verifiedBy = verifiedBy;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(Instant verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public static AIScanBuilder builder() {
    return new AIScanBuilder();
  }

  public static class AIScanBuilder {
    private Animal animal;
    private User uploadedBy;
    private String imageUrl;
    private String imageHash;
    private AIProviderType aiProvider = AIProviderType.NONE;
    private String aiModel;
    private String diagnosis;
    private BigDecimal confidenceScore;
    private AIScanStatus status = AIScanStatus.PENDING;
    private boolean veterinarianVerified = false;
    private User verifiedBy;
    private Instant verifiedAt;
    private String notes;
    private Long version = 0L;

    public AIScanBuilder animal(Animal animal) {
      this.animal = animal;
      return this;
    }

    public AIScanBuilder uploadedBy(User uploadedBy) {
      this.uploadedBy = uploadedBy;
      return this;
    }

    public AIScanBuilder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public AIScanBuilder imageHash(String imageHash) {
      this.imageHash = imageHash;
      return this;
    }

    public AIScanBuilder aiProvider(AIProviderType aiProvider) {
      this.aiProvider = aiProvider;
      return this;
    }

    public AIScanBuilder aiModel(String aiModel) {
      this.aiModel = aiModel;
      return this;
    }

    public AIScanBuilder diagnosis(String diagnosis) {
      this.diagnosis = diagnosis;
      return this;
    }

    public AIScanBuilder confidenceScore(BigDecimal confidenceScore) {
      this.confidenceScore = confidenceScore;
      return this;
    }

    public AIScanBuilder status(AIScanStatus status) {
      this.status = status;
      return this;
    }

    public AIScanBuilder veterinarianVerified(boolean veterinarianVerified) {
      this.veterinarianVerified = veterinarianVerified;
      return this;
    }

    public AIScanBuilder verifiedBy(User verifiedBy) {
      this.verifiedBy = verifiedBy;
      return this;
    }

    public AIScanBuilder verifiedAt(Instant verifiedAt) {
      this.verifiedAt = verifiedAt;
      return this;
    }

    public AIScanBuilder notes(String notes) {
      this.notes = notes;
      return this;
    }

    public AIScanBuilder version(Long version) {
      this.version = version;
      return this;
    }

    public AIScan build() {
      AIScan scan = new AIScan();
      scan.setAnimal(this.animal);
      scan.setUploadedBy(this.uploadedBy);
      scan.setImageUrl(this.imageUrl);
      scan.setImageHash(this.imageHash);
      scan.setAiProvider(this.aiProvider);
      scan.setAiModel(this.aiModel);
      scan.setDiagnosis(this.diagnosis);
      scan.setConfidenceScore(this.confidenceScore);
      scan.setStatus(this.status);
      scan.setVeterinarianVerified(this.veterinarianVerified);
      scan.setVerifiedBy(this.verifiedBy);
      scan.setVerifiedAt(this.verifiedAt);
      scan.setNotes(this.notes);
      scan.setVersion(this.version);
      return scan;
    }
  }
}
