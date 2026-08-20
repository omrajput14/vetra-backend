package app.vetra.ai.entity;

import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.BaseEntity;
import app.vetra.infrastructure.persistence.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Entity representing an interactive AI Veterinary Advisor conversational session. */
@Entity
@Table(name = "ai_advisor_sessions")
public class AIAdvisorSession extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false)
  private Animal animal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private AIAdvisorSessionStatus status = AIAdvisorSessionStatus.QUESTIONING;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level", length = 50)
  private AIAdvisorRiskLevel riskLevel = AIAdvisorRiskLevel.UNKNOWN;

  @Column(name = "requires_vet_review", nullable = false)
  private boolean requiresVetReview = true;

  @Column(name = "assessment_json", columnDefinition = "TEXT")
  private String assessmentJson;

  @Column(name = "turn_count", nullable = false)
  private int turnCount = 0;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<AIAdvisorMessage> messages = new ArrayList<>();

  /** Default constructor. */
  public AIAdvisorSession() {}

  /**
   * Appends a message to this session.
   *
   * @param message message to add
   */
  public void addMessage(AIAdvisorMessage message) {
    if (messages == null) {
      messages = new ArrayList<>();
    }
    messages.add(message);
    message.setSession(this);
  }

  public Animal getAnimal() {
    return animal;
  }

  public void setAnimal(Animal animal) {
    this.animal = animal;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public AIAdvisorSessionStatus getStatus() {
    return status;
  }

  public void setStatus(AIAdvisorSessionStatus status) {
    this.status = status;
  }

  public AIAdvisorRiskLevel getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(AIAdvisorRiskLevel riskLevel) {
    this.riskLevel = riskLevel;
  }

  public boolean isRequiresVetReview() {
    return requiresVetReview;
  }

  public void setRequiresVetReview(boolean requiresVetReview) {
    this.requiresVetReview = requiresVetReview;
  }

  public String getAssessmentJson() {
    return assessmentJson;
  }

  public void setAssessmentJson(String assessmentJson) {
    this.assessmentJson = assessmentJson;
  }

  public int getTurnCount() {
    return turnCount;
  }

  public void setTurnCount(int turnCount) {
    this.turnCount = turnCount;
  }

  public List<AIAdvisorMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<AIAdvisorMessage> messages) {
    this.messages = messages;
  }

  /** Builder pattern implementation. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder class. */
  public static class Builder {
    private final AIAdvisorSession session = new AIAdvisorSession();

    public Builder animal(Animal animal) {
      session.setAnimal(animal);
      return this;
    }

    public Builder user(User user) {
      session.setUser(user);
      return this;
    }

    public Builder status(AIAdvisorSessionStatus status) {
      session.setStatus(status);
      return this;
    }

    public Builder riskLevel(AIAdvisorRiskLevel riskLevel) {
      session.setRiskLevel(riskLevel);
      return this;
    }

    public Builder requiresVetReview(boolean requiresVetReview) {
      session.setRequiresVetReview(requiresVetReview);
      return this;
    }

    public Builder assessmentJson(String assessmentJson) {
      session.setAssessmentJson(assessmentJson);
      return this;
    }

    public Builder turnCount(int turnCount) {
      session.setTurnCount(turnCount);
      return this;
    }

    public AIAdvisorSession build() {
      return session;
    }
  }
}
