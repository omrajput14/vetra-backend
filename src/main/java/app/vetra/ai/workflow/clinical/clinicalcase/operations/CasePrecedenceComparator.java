package app.vetra.ai.workflow.clinical.clinicalcase.operations;

import app.vetra.ai.workflow.clinical.model.TriageUrgency;
import java.time.Instant;
import java.util.Comparator;

/**
 * Centralized, deterministic comparator enforcing operational precedence and stable pagination order.
 *
 * <p>Ordering sequence:
 * 1. Operational status precedence rank (descending)
 * 2. Triage urgency rank (descending: EMERGENCY > URGENT > PRIORITY > ROUTINE)
 * 3. Next due timestamp (earliest first)
 * 4. Last encounter timestamp (earliest first)
 * 5. Case UUID string (stable tie-breaker)
 */
public class CasePrecedenceComparator implements Comparator<ClinicalCaseWorkQueueItem> {

  public static final CasePrecedenceComparator INSTANCE = new CasePrecedenceComparator();

  @Override
  public int compare(ClinicalCaseWorkQueueItem item1, ClinicalCaseWorkQueueItem item2) {
    if (item1 == item2) {
      return 0;
    }
    if (item1 == null) {
      return 1;
    }
    if (item2 == null) {
      return -1;
    }

    int rank1 = item1.operationalStatus() != null ? item1.operationalStatus().getPrecedenceRank() : 0;
    int rank2 = item2.operationalStatus() != null ? item2.operationalStatus().getPrecedenceRank() : 0;
    if (rank1 != rank2) {
      return Integer.compare(rank2, rank1);
    }

    int urg1 = getUrgencyRank(item1.latestUrgency());
    int urg2 = getUrgencyRank(item2.latestUrgency());
    if (urg1 != urg2) {
      return Integer.compare(urg2, urg1);
    }

    int dueCompare = compareInstants(item1.nextDueAt(), item2.nextDueAt());
    if (dueCompare != 0) {
      return dueCompare;
    }

    int encCompare = compareInstants(item1.lastEncounterAt(), item2.lastEncounterAt());
    if (encCompare != 0) {
      return encCompare;
    }

    return item1.caseId().compareTo(item2.caseId());
  }

  private int getUrgencyRank(TriageUrgency urgency) {
    if (urgency == null) {
      return 0;
    }
    return switch (urgency) {
      case EMERGENCY -> 4;
      case URGENT -> 3;
      case PRIORITY -> 2;
      case ROUTINE -> 1;
    };
  }

  private int compareInstants(Instant t1, Instant t2) {
    if (t1 == null && t2 == null) {
      return 0;
    }
    if (t1 == null) {
      return 1;
    }
    if (t2 == null) {
      return -1;
    }
    return t1.compareTo(t2);
  }
}
