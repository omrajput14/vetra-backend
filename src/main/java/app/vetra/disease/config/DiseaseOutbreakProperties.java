package app.vetra.disease.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed Spring Boot properties for disease outbreak thresholds and disease-specific
 * profiles.
 */
@Configuration
@ConfigurationProperties(prefix = "vetra.disease.outbreak")
public class DiseaseOutbreakProperties {

  private int evaluationWindowHours = 72;
  private double defaultRadiusKm = 15.0;
  private int defaultMinimumConfirmedCases = 3;

  private Map<String, ProfileConfig> profiles = new HashMap<>();

  public int getEvaluationWindowHours() {
    return evaluationWindowHours;
  }

  public void setEvaluationWindowHours(int evaluationWindowHours) {
    this.evaluationWindowHours = evaluationWindowHours;
  }

  public double getDefaultRadiusKm() {
    return defaultRadiusKm;
  }

  public void setDefaultRadiusKm(double defaultRadiusKm) {
    this.defaultRadiusKm = defaultRadiusKm;
  }

  public int getDefaultMinimumConfirmedCases() {
    return defaultMinimumConfirmedCases;
  }

  public void setDefaultMinimumConfirmedCases(int defaultMinimumConfirmedCases) {
    this.defaultMinimumConfirmedCases = defaultMinimumConfirmedCases;
  }

  public Map<String, ProfileConfig> getProfiles() {
    return profiles;
  }

  public void setProfiles(Map<String, ProfileConfig> profiles) {
    this.profiles = profiles;
  }

  /**
   * Resolves a disease profile for the specified disease name, or returns disease-specific
   * defaults.
   *
   * @param diseaseName name of disease
   * @return {@link DiseaseProfile} instance
   */
  public DiseaseProfile getProfileForDisease(String diseaseName) {
    if (diseaseName == null) {
      return DiseaseProfile.defaultProfile("UNKNOWN");
    }

    String key = diseaseName.trim().toLowerCase();

    // Check predefined profiles first
    if (key.contains("foot") || key.contains("fmd")) {
      return new DiseaseProfile(diseaseName, 25.0, 3, 48, 1.8, "HIGH");
    } else if (key.contains("rabies")) {
      return new DiseaseProfile(diseaseName, 50.0, 1, 24, 2.5, "URGENT");
    } else if (key.contains("brucellosis")) {
      return new DiseaseProfile(diseaseName, 10.0, 5, 168, 1.2, "MEDIUM");
    } else if (key.contains("anthrax")) {
      return new DiseaseProfile(diseaseName, 30.0, 1, 24, 3.0, "CRITICAL");
    }

    ProfileConfig cfg = profiles.get(key);
    if (cfg != null) {
      return new DiseaseProfile(
          diseaseName,
          cfg.getRadiusKm() > 0 ? cfg.getRadiusKm() : defaultRadiusKm,
          cfg.getMinimumConfirmedCases() > 0
              ? cfg.getMinimumConfirmedCases()
              : defaultMinimumConfirmedCases,
          cfg.getEvaluationWindowHours() > 0
              ? cfg.getEvaluationWindowHours()
              : evaluationWindowHours,
          cfg.getSeverityWeight() > 0 ? cfg.getSeverityWeight() : 1.0,
          cfg.getReportPriority() != null ? cfg.getReportPriority() : "MEDIUM");
    }

    return DiseaseProfile.defaultProfile(diseaseName);
  }

  /** Inner configuration class mapping individual YAML profile properties. */
  public static class ProfileConfig {
    private double radiusKm;
    private int minimumConfirmedCases;
    private int evaluationWindowHours;
    private double severityWeight = 1.0;
    private String reportPriority = "MEDIUM";

    public double getRadiusKm() {
      return radiusKm;
    }

    public void setRadiusKm(double radiusKm) {
      this.radiusKm = radiusKm;
    }

    public int getMinimumConfirmedCases() {
      return minimumConfirmedCases;
    }

    public void setMinimumConfirmedCases(int minimumConfirmedCases) {
      this.minimumConfirmedCases = minimumConfirmedCases;
    }

    public int getEvaluationWindowHours() {
      return evaluationWindowHours;
    }

    public void setEvaluationWindowHours(int evaluationWindowHours) {
      this.evaluationWindowHours = evaluationWindowHours;
    }

    public double getSeverityWeight() {
      return severityWeight;
    }

    public void setSeverityWeight(double severityWeight) {
      this.severityWeight = severityWeight;
    }

    public String getReportPriority() {
      return reportPriority;
    }

    public void setReportPriority(String reportPriority) {
      this.reportPriority = reportPriority;
    }
  }
}
