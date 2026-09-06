package app.vetra.dashboard.dto;

import java.math.BigDecimal;

/**
 * Standard response DTO for modeled economic impact and livestock loss-avoidance calculations.
 *
 * <p>All financial metrics are strictly marked as modeled estimates and must not be presented
 * as audited financial statements or guaranteed savings.
 *
 * @param modeledSavings numerical estimated savings in INR, or null if insufficient reliable data
 * @param formattedValue formatted string representation (e.g. "₹18,000") or null
 * @param unit currency unit (default "INR")
 * @param label display badge label (always "Modeled estimate")
 * @param isModeled whether the value represents a modeled estimate (always true)
 * @param hasSufficientData whether sufficient reliable animal telemetry exists for estimation
 * @param eligibleAnimalsCount number of distinct eligible animals included in calculation
 * @param statusMessage user-facing status description or unavailable explanation
 * @param methodology description of the modeling assumptions
 * @param methodologyVersion version tag of the calculation algorithm
 * @param scope calculation scope ("FARMER" or "STATEWIDE")
 */
public record EconomicImpactResponse(
    BigDecimal modeledSavings,
    String formattedValue,
    String unit,
    String label,
    boolean isModeled,
    boolean hasSufficientData,
    long eligibleAnimalsCount,
    String statusMessage,
    String methodology,
    String methodologyVersion,
    String scope) {

  private static final String DEFAULT_LABEL = "Modeled estimate";
  private static final String DEFAULT_UNIT = "INR";
  private static final String DEFAULT_METHODOLOGY =
      "Modeled estimate based on registered livestock and early-detection outcomes; not an audited financial measure.";
  private static final String METHODOLOGY_VERSION = "v1.0-deterministic";

  /**
   * Factory method for successful estimation with sufficient data.
   */
  public static EconomicImpactResponse ofCalculated(
      BigDecimal savings,
      long eligibleCount,
      String scope) {
    String formatted = String.format("₹%,d", savings.longValue());
    return new EconomicImpactResponse(
        savings,
        formatted,
        DEFAULT_UNIT,
        DEFAULT_LABEL,
        true,
        true,
        eligibleCount,
        "Modeled from registered livestock data and early-detection assumptions.",
        DEFAULT_METHODOLOGY,
        METHODOLOGY_VERSION,
        scope);
  }

  /**
   * Factory method for insufficient reliable data state.
   */
  public static EconomicImpactResponse ofInsufficientData(String scope, String statusMessage) {
    return new EconomicImpactResponse(
        null,
        null,
        DEFAULT_UNIT,
        DEFAULT_LABEL,
        true,
        false,
        0L,
        statusMessage,
        DEFAULT_METHODOLOGY,
        METHODOLOGY_VERSION,
        scope);
  }
}
