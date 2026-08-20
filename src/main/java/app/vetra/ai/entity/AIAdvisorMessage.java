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

/** Entity representing a single message turn within an AI Veterinary Advisor session. */
@Entity
@Table(name = "ai_advisor_messages")
public class AIAdvisorMessage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private AIAdvisorSession session;

  @Enumerated(EnumType.STRING)
  @Column(name = "sender_type", nullable = false, length = 20)
  private AIAdvisorSenderType senderType;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "structured_payload", columnDefinition = "TEXT")
  private String structuredPayload;

  @Column(name = "turn_number", nullable = false)
  private int turnNumber = 0;

  /** Default constructor. */
  public AIAdvisorMessage() {}

  public AIAdvisorSession getSession() {
    return session;
  }

  public void setSession(AIAdvisorSession session) {
    this.session = session;
  }

  public AIAdvisorSenderType getSenderType() {
    return senderType;
  }

  public void setSenderType(AIAdvisorSenderType senderType) {
    this.senderType = senderType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getStructuredPayload() {
    return structuredPayload;
  }

  public void setStructuredPayload(String structuredPayload) {
    this.structuredPayload = structuredPayload;
  }

  public int getTurnNumber() {
    return turnNumber;
  }

  public void setTurnNumber(int turnNumber) {
    this.turnNumber = turnNumber;
  }

  /** Builder pattern implementation. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class. */
  public static class Builder {
    private final AIAdvisorMessage message = new AIAdvisorMessage();

    public Builder session(AIAdvisorSession session) {
      message.setSession(session);
      return this;
    }

    public Builder senderType(AIAdvisorSenderType senderType) {
      message.setSenderType(senderType);
      return this;
    }

    public Builder content(String content) {
      message.setContent(content);
      return this;
    }

    public Builder structuredPayload(String structuredPayload) {
      message.setStructuredPayload(structuredPayload);
      return this;
    }

    public Builder turnNumber(int turnNumber) {
      message.setTurnNumber(turnNumber);
      return this;
    }

    public AIAdvisorMessage build() {
      return message;
    }
  }
}
