package app.vetra.ai.entity;

import app.vetra.infrastructure.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audit trail entity storing individual AI provider inference iteration results for an AIScan. */
@Entity
@Table(name = "ai_scan_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIScanResultEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "scan_id", nullable = false)
  private AIScan scan;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 32)
  private AIProviderType provider;

  @Column(name = "model", length = 64)
  private String model;

  @Column(name = "diagnosis", columnDefinition = "TEXT")
  private String diagnosis;

  @Column(name = "confidence", precision = 4, scale = 3)
  private BigDecimal confidence;

  @Column(name = "raw_response", columnDefinition = "TEXT")
  private String rawResponse;

  @Column(name = "latency_ms", nullable = false)
  @Builder.Default
  private long latencyMs = 0L;

  @Column(name = "request_id", length = 64)
  private String requestId;

  @Column(name = "tokens_used")
  private Integer tokensUsed;

  @Column(name = "warnings", columnDefinition = "TEXT")
  private String warnings;

  public AIScan getScan() {
    return scan;
  }

  public void setScan(AIScan scan) {
    this.scan = scan;
  }

  public AIProviderType getProvider() {
    return provider;
  }

  public void setProvider(AIProviderType provider) {
    this.provider = provider;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public void setDiagnosis(String diagnosis) {
    this.diagnosis = diagnosis;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public void setConfidence(BigDecimal confidence) {
    this.confidence = confidence;
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }

  public long getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Integer getTokensUsed() {
    return tokensUsed;
  }

  public void setTokensUsed(Integer tokensUsed) {
    this.tokensUsed = tokensUsed;
  }

  public String getWarnings() {
    return warnings;
  }

  public void setWarnings(String warnings) {
    this.warnings = warnings;
  }

  public static AIScanResultEntityBuilder builder() {
    return new AIScanResultEntityBuilder();
  }

  public static class AIScanResultEntityBuilder {
    private AIScan scan;
    private AIProviderType provider;
    private String model;
    private String diagnosis;
    private BigDecimal confidence;
    private String rawResponse;
    private long latencyMs = 0L;
    private String requestId;
    private Integer tokensUsed;
    private String warnings;

    public AIScanResultEntityBuilder scan(AIScan scan) {
      this.scan = scan;
      return this;
    }

    public AIScanResultEntityBuilder provider(AIProviderType provider) {
      this.provider = provider;
      return this;
    }

    public AIScanResultEntityBuilder model(String model) {
      this.model = model;
      return this;
    }

    public AIScanResultEntityBuilder diagnosis(String diagnosis) {
      this.diagnosis = diagnosis;
      return this;
    }

    public AIScanResultEntityBuilder confidence(BigDecimal confidence) {
      this.confidence = confidence;
      return this;
    }

    public AIScanResultEntityBuilder rawResponse(String rawResponse) {
      this.rawResponse = rawResponse;
      return this;
    }

    public AIScanResultEntityBuilder latencyMs(long latencyMs) {
      this.latencyMs = latencyMs;
      return this;
    }

    public AIScanResultEntityBuilder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public AIScanResultEntityBuilder tokensUsed(Integer tokensUsed) {
      this.tokensUsed = tokensUsed;
      return this;
    }

    public AIScanResultEntityBuilder warnings(String warnings) {
      this.warnings = warnings;
      return this;
    }

    public AIScanResultEntity build() {
      AIScanResultEntity entity = new AIScanResultEntity();
      entity.setScan(this.scan);
      entity.setProvider(this.provider);
      entity.setModel(this.model);
      entity.setDiagnosis(this.diagnosis);
      entity.setConfidence(this.confidence);
      entity.setRawResponse(this.rawResponse);
      entity.setLatencyMs(this.latencyMs);
      entity.setRequestId(this.requestId);
      entity.setTokensUsed(this.tokensUsed);
      entity.setWarnings(this.warnings);
      return entity;
    }
  }
}
