package app.vetra.ai.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Configuration properties for AI Governance (safety, policy, budget, and auditing). */
public class GovernanceProperties {

  private boolean enabled = true;
  private SafetyConfig safety = new SafetyConfig();
  private PolicyConfig policy = new PolicyConfig();
  private BudgetConfig budget = new BudgetConfig();
  private AuditConfig audit = new AuditConfig();

  /** Default constructor. */
  public GovernanceProperties() {}

  /**
   * Returns enabled status.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets enabled status.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns safety config.
   *
   * @return safety config object
   */
  public SafetyConfig getSafety() {
    return safety;
  }

  /**
   * Sets safety config.
   *
   * @param safety safety config object
   */
  public void setSafety(SafetyConfig safety) {
    this.safety = safety != null ? safety : new SafetyConfig();
  }

  /**
   * Returns policy config.
   *
   * @return policy config object
   */
  public PolicyConfig getPolicy() {
    return policy;
  }

  /**
   * Sets policy config.
   *
   * @param policy policy config object
   */
  public void setPolicy(PolicyConfig policy) {
    this.policy = policy != null ? policy : new PolicyConfig();
  }

  /**
   * Returns budget config.
   *
   * @return budget config object
   */
  public BudgetConfig getBudget() {
    return budget;
  }

  /**
   * Sets budget config.
   *
   * @param budget budget config object
   */
  public void setBudget(BudgetConfig budget) {
    this.budget = budget != null ? budget : new BudgetConfig();
  }

  /**
   * Returns audit config.
   *
   * @return audit config object
   */
  public AuditConfig getAudit() {
    return audit;
  }

  /**
   * Sets audit config.
   *
   * @param audit audit config object
   */
  public void setAudit(AuditConfig audit) {
    this.audit = audit != null ? audit : new AuditConfig();
  }

  // ── Nested: SafetyConfig ────────────────────────────────────────────────

  /** Configuration for safety filtering. */
  public static final class SafetyConfig {
    private boolean enabled = true;
    private List<String> blockedKeywords = new ArrayList<>();
    private String strictness = "STRICT";

    /** Default constructor. */
    public SafetyConfig() {}

    /**
     * Returns true if safety filtering enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets safety filter enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns list of blocked keywords.
     *
     * @return list of keyword strings
     */
    public List<String> getBlockedKeywords() {
      return blockedKeywords;
    }

    /**
     * Sets blocked keywords list.
     *
     * @param blockedKeywords list of keyword strings
     */
    public void setBlockedKeywords(List<String> blockedKeywords) {
      this.blockedKeywords = blockedKeywords != null ? blockedKeywords : new ArrayList<>();
    }

    /**
     * Returns strictness level.
     *
     * @return strictness string
     */
    public String getStrictness() {
      return strictness;
    }

    /**
     * Sets strictness level.
     *
     * @param strictness strictness level string
     */
    public void setStrictness(String strictness) {
      this.strictness = strictness;
    }
  }

  // ── Nested: PolicyConfig ────────────────────────────────────────────────

  /** Configuration for enterprise policy engine. */
  public static final class PolicyConfig {
    private boolean enabled = true;
    private Map<String, List<String>> tenantAllowedProviders = new HashMap<>();
    private Map<String, List<String>> tenantAllowedModels = new HashMap<>();
    private int maxPromptTokens = 32768;

    /** Default constructor. */
    public PolicyConfig() {}

    /**
     * Returns true if policy engine enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets policy engine enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns map of tenant allowed providers.
     *
     * @return tenant provider restrictions map
     */
    public Map<String, List<String>> getTenantAllowedProviders() {
      return tenantAllowedProviders;
    }

    /**
     * Sets tenant allowed providers map.
     *
     * @param tenantAllowedProviders map of tenant restrictions
     */
    public void setTenantAllowedProviders(Map<String, List<String>> tenantAllowedProviders) {
      this.tenantAllowedProviders =
          tenantAllowedProviders != null ? tenantAllowedProviders : new HashMap<>();
    }

    /**
     * Returns map of tenant allowed models.
     *
     * @return tenant model restrictions map
     */
    public Map<String, List<String>> getTenantAllowedModels() {
      return tenantAllowedModels;
    }

    /**
     * Sets tenant allowed models map.
     *
     * @param tenantAllowedModels map of tenant model restrictions
     */
    public void setTenantAllowedModels(Map<String, List<String>> tenantAllowedModels) {
      this.tenantAllowedModels =
          tenantAllowedModels != null ? tenantAllowedModels : new HashMap<>();
    }

    /**
     * Returns max prompt token bound.
     *
     * @return max tokens
     */
    public int getMaxPromptTokens() {
      return maxPromptTokens;
    }

    /**
     * Sets max prompt token bound.
     *
     * @param maxPromptTokens max token limit
     */
    public void setMaxPromptTokens(int maxPromptTokens) {
      this.maxPromptTokens = maxPromptTokens;
    }
  }

  // ── Nested: BudgetConfig ────────────────────────────────────────────────

  /** Configuration for token/cost budget enforcement. */
  public static final class BudgetConfig {
    private boolean enabled = true;
    private Map<String, Long> tenantDailyTokenLimit = new HashMap<>();
    private Map<String, Double> costPer1kTokens = new HashMap<>();

    /** Default constructor. */
    public BudgetConfig() {}

    /**
     * Returns true if budget manager enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets budget manager enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns map of tenant daily token limits.
     *
     * @return limit map
     */
    public Map<String, Long> getTenantDailyTokenLimit() {
      return tenantDailyTokenLimit;
    }

    /**
     * Sets tenant daily token limit map.
     *
     * @param tenantDailyTokenLimit limit map
     */
    public void setTenantDailyTokenLimit(Map<String, Long> tenantDailyTokenLimit) {
      this.tenantDailyTokenLimit =
          tenantDailyTokenLimit != null ? tenantDailyTokenLimit : new HashMap<>();
    }

    /**
     * Returns cost per 1k tokens map.
     *
     * @return cost map
     */
    public Map<String, Double> getCostPer1kTokens() {
      return costPer1kTokens;
    }

    /**
     * Sets cost per 1k tokens map.
     *
     * @param costPer1kTokens cost map
     */
    public void setCostPer1kTokens(Map<String, Double> costPer1kTokens) {
      this.costPer1kTokens = costPer1kTokens != null ? costPer1kTokens : new HashMap<>();
    }
  }

  // ── Nested: AuditConfig ─────────────────────────────────────────────────

  /** Configuration for audit logging. */
  public static final class AuditConfig {
    private boolean enabled = true;
    private boolean logPromptContent = false;

    /** Default constructor. */
    public AuditConfig() {}

    /**
     * Returns true if audit service enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets audit service enabled status.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Returns true if prompt content logging is enabled.
     *
     * @return true if prompt content should be logged
     */
    public boolean isLogPromptContent() {
      return logPromptContent;
    }

    /**
     * Sets prompt content logging status.
     *
     * @param logPromptContent true to enable prompt content logging
     */
    public void setLogPromptContent(boolean logPromptContent) {
      this.logPromptContent = logPromptContent;
    }
  }
}
