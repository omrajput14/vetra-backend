package app.vetra.developer.dto;

import java.util.List;
import java.util.Map;

/** Farmer analytics deep-dive payload. */
public record DeveloperFarmerAnalyticsDto(
    long totalFarmers,
    long activeFarmers,
    long newFarmersInPeriod,
    long totalAnimals,
    Map<String, Long> speciesDistribution,
    long vaccinatedAnimalsCount,
    long unvaccinatedAnimalsCount,
    double vaccinationRatePercent,
    long diseaseReportsSubmitted,
    long aiScansSubmitted,
    long appointmentsBooked,
    long totalVaccinationRecords,
    Map<String, Long> topDistricts,
    List<TimeSeriesPointDto> registrationTrend,
    String timeRange) {}
