package app.vetra.vaccination.enums;

import java.util.Set;

/**
 * Operational status lifecycle for a government vaccination campaign.
 */
public enum CampaignStatus {
  PLANNED,
  ACTIVE,
  COMPLETED,
  CANCELLED;

  /**
   * Validates whether a lifecycle status transition is permitted.
   *
   * @param targetStatus the desired new status
   * @return true if permitted, false otherwise
   */
  public boolean canTransitionTo(CampaignStatus targetStatus) {
    if (this == targetStatus) {
      return true;
    }
    return switch (this) {
      case PLANNED -> targetStatus == ACTIVE || targetStatus == CANCELLED;
      case ACTIVE -> targetStatus == COMPLETED || targetStatus == CANCELLED;
      case COMPLETED, CANCELLED -> false;
    };
  }

  /**
   * Returns valid transition targets from this status.
   *
   * @return Set of allowed next states
   */
  public Set<CampaignStatus> validTransitions() {
    return switch (this) {
      case PLANNED -> Set.of(ACTIVE, CANCELLED);
      case ACTIVE -> Set.of(COMPLETED, CANCELLED);
      case COMPLETED, CANCELLED -> Set.of();
    };
  }
}
