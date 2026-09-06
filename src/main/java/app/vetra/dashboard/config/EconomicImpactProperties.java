package app.vetra.dashboard.config;

import app.vetra.infrastructure.persistence.enums.Species;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for modeled livestock economic savings and loss avoidance.
 *
 * <p>IMPORTANT: These values represent reference modeling assumptions derived from Indian
 * livestock economic benchmarks (e.g. NABARD / Department of Animal Husbandry and Dairying
 * standard unit costs) and an empirical early-detection loss-avoidance coefficient. They are
 * NOT audited financial statements or guaranteed monetary returns.
 */
@Configuration
@ConfigurationProperties(prefix = "vetra.economic-impact")
public class EconomicImpactProperties {

  /**
   * Modeled prevention / loss-avoidance factor achieved through early AI screening and timely
   * veterinary intervention. Default: 0.40 (40% loss exposure avoided).
   */
  private BigDecimal preventionFactor = new BigDecimal("0.40");

  /**
   * Baseline asset replacement reference values by species in Indian Rupees (INR).
   */
  private Map<String, BigDecimal> referenceValues = new HashMap<>();

  public EconomicImpactProperties() {
    // Standard reference unit values (INR)
    referenceValues.put("cattle", new BigDecimal("45000"));
    referenceValues.put("buffalo", new BigDecimal("55000"));
    referenceValues.put("sheep", new BigDecimal("7000"));
    referenceValues.put("goat", new BigDecimal("8000"));
    referenceValues.put("swine", new BigDecimal("12000"));
    referenceValues.put("poultry", new BigDecimal("350"));
    referenceValues.put("other", new BigDecimal("10000"));
  }

  public BigDecimal getPreventionFactor() {
    return preventionFactor;
  }

  public void setPreventionFactor(BigDecimal preventionFactor) {
    this.preventionFactor = preventionFactor;
  }

  public Map<String, BigDecimal> getReferenceValues() {
    return referenceValues;
  }

  public void setReferenceValues(Map<String, BigDecimal> referenceValues) {
    this.referenceValues = referenceValues;
  }

  /**
   * Resolves reference asset value for a given livestock species.
   *
   * @param species livestock species
   * @return configured reference value in INR, or default fallback
   */
  public BigDecimal getReferenceValue(Species species) {
    if (species == null) {
      return referenceValues.getOrDefault("other", new BigDecimal("10000"));
    }
    String key = species.name().toLowerCase();
    return referenceValues.getOrDefault(key, referenceValues.getOrDefault("other", new BigDecimal("10000")));
  }
}
