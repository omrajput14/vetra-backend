package app.vetra.developer.service;

import app.vetra.ai.entity.AIScan;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.developer.dto.DeveloperActivityEventDto;
import app.vetra.disease.entity.DiseaseReport;
import app.vetra.disease.repository.DiseaseReportRepository;
import app.vetra.infrastructure.persistence.entity.Appointment;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service responsible for aggregating live operational activity events. */
@Service
public class DeveloperActivityFeedService {

  private final UserRepository userRepository;
  private final AIScanRepository aiScanRepository;
  private final DiseaseReportRepository diseaseReportRepository;
  private final AppointmentRepository appointmentRepository;
  private final MedicalRecordRepository medicalRecordRepository;

  public DeveloperActivityFeedService(
      UserRepository userRepository,
      AIScanRepository aiScanRepository,
      DiseaseReportRepository diseaseReportRepository,
      AppointmentRepository appointmentRepository,
      MedicalRecordRepository medicalRecordRepository) {
    this.userRepository = userRepository;
    this.aiScanRepository = aiScanRepository;
    this.diseaseReportRepository = diseaseReportRepository;
    this.appointmentRepository = appointmentRepository;
    this.medicalRecordRepository = medicalRecordRepository;
  }

  @Transactional(readOnly = true)
  public List<DeveloperActivityEventDto> getActivityFeed(int limit) {
    int maxItems = Math.min(100, Math.max(5, limit));
    List<DeveloperActivityEventDto> events = new ArrayList<>();

    appendUserRegistrations(events);
    appendAiScans(events);
    appendDiseaseReports(events);
    appendAppointments(events);
    appendMedicalRecords(events);

    events.sort(Comparator.comparing(DeveloperActivityEventDto::timestamp, Comparator.nullsLast(Comparator.reverseOrder())));
    return events.stream().limit(maxItems).collect(Collectors.toList());
  }

  private void appendUserRegistrations(List<DeveloperActivityEventDto> events) {
    for (User u : userRepository.findTop30ByOrderByCreatedAtDesc()) {
      if (u.getCreatedAt() != null) {
        events.add(
            new DeveloperActivityEventDto(
                "USR-" + u.getId().toString().substring(0, 8),
                "USER_REGISTERED",
                u.getRole().name(),
                maskIdentifier(u.getEmail() != null ? u.getEmail() : u.getPhone()),
                "New " + u.getRole().name() + " Registered",
                "Account created with language: " + u.getPreferredLanguage(),
                u.isActive() ? "ACTIVE" : "INACTIVE",
                u.getCreatedAt(),
                u.getId().toString(),
                "AUTH"));
      }
    }
  }

  private void appendAiScans(List<DeveloperActivityEventDto> events) {
    for (AIScan s : aiScanRepository.findTop30ByOrderByCreatedAtDesc()) {
      if (s.getCreatedAt() != null) {
        String diag = s.getDiagnosis() != null ? s.getDiagnosis() : "Processing";
        events.add(
            new DeveloperActivityEventDto(
                "SCN-" + s.getId().toString().substring(0, 8),
                "AI_SCAN_" + s.getStatus().name(),
                "SYSTEM",
                s.getUploadedBy() != null ? maskIdentifier(s.getUploadedBy().getEmail()) : "ANONYMOUS",
                "AI Scan " + s.getStatus().name(),
                "Diagnosis: " + diag,
                s.getStatus().name(),
                s.getCreatedAt(),
                s.getId().toString(),
                "DIAGNOSTICS"));
      }
    }
  }

  private void appendDiseaseReports(List<DeveloperActivityEventDto> events) {
    for (DiseaseReport r : diseaseReportRepository.findTop30ByOrderByCreatedAtDesc()) {
      if (r.getCreatedAt() != null) {
        String userMasked = r.getReportedBy() != null ? maskIdentifier(r.getReportedBy().getEmail()) : "ANONYMOUS";
        String species = r.getAnimal() != null && r.getAnimal().getSpecies() != null ? r.getAnimal().getSpecies().name() : "LIVESTOCK";
        events.add(
            new DeveloperActivityEventDto(
                "REP-" + r.getId().toString().substring(0, 8),
                "DISEASE_REPORT_" + r.getDiagnosisStatus().name(),
                r.getReportSource() != null ? r.getReportSource().name() : "FIELD",
                userMasked,
                "Disease Report: " + r.getDiseaseName(),
                "Species: " + species + ", Status: " + r.getDiagnosisStatus().name(),
                r.getDiagnosisStatus().name(),
                r.getCreatedAt(),
                r.getId().toString(),
                "SURVEILLANCE"));
      }
    }
  }

  private void appendAppointments(List<DeveloperActivityEventDto> events) {
    for (Appointment a : appointmentRepository.findTop30ByOrderByCreatedAtDesc()) {
      if (a.getCreatedAt() != null) {
        events.add(
            new DeveloperActivityEventDto(
                "APT-" + a.getId().toString().substring(0, 8),
                "APPOINTMENT_" + a.getStatus().name(),
                "COORDINATION",
                a.getFarmer() != null ? maskIdentifier(a.getFarmer().getFullName()) : "FARMER",
                "Appointment " + a.getStatus().name(),
                "Visit Type: " + a.getVisitType() + ", Date: " + a.getAppointmentDate(),
                a.getStatus().name(),
                a.getCreatedAt(),
                a.getId().toString(),
                "CLINICAL"));
      }
    }
  }

  private void appendMedicalRecords(List<DeveloperActivityEventDto> events) {
    for (MedicalRecord m : medicalRecordRepository.findTop30ByOrderByCreatedAtDesc()) {
      if (m.getCreatedAt() != null) {
        events.add(
            new DeveloperActivityEventDto(
                "MED-" + m.getId().toString().substring(0, 8),
                "MEDICAL_RECORD_CREATED",
                "VETERINARIAN",
                m.getVeterinarian() != null ? maskIdentifier(m.getVeterinarian().getFullName()) : "VET",
                "Medical Record Written",
                "Diagnosis: " + m.getDiagnosis(),
                "COMPLETED",
                m.getCreatedAt().toInstant(),
                m.getId().toString(),
                "CLINICAL"));
      }
    }
  }

  private String maskIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return "Anonymous";
    }
    if (value.contains("@")) {
      String[] parts = value.split("@");
      String name = parts[0];
      String masked = name.length() <= 2 ? name + "***" : name.substring(0, 2) + "***" + name.charAt(name.length() - 1);
      return masked + "@" + parts[1];
    }
    if (value.length() >= 7) {
      return value.substring(0, 3) + "****" + value.substring(value.length() - 2);
    }
    return value;
  }
}
