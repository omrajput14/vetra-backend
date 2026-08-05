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
}
