package app.vetra.ai.service;

import app.vetra.ai.entity.AIAdvisorMessage;
import app.vetra.ai.entity.AIAdvisorSenderType;
import app.vetra.ai.entity.AIScan;
import app.vetra.ai.repository.AIScanRepository;
import app.vetra.infrastructure.persistence.entity.Animal;
import app.vetra.infrastructure.persistence.entity.MedicalRecord;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service responsible for assembling sanitized, relevant, and structured clinical context for the AI
 * Veterinary Advisor without leaking sensitive tokens or infrastructure metadata.
 */
@Service
public class AIAdvisorContextBuilder {

  private static final int MAX_RECENT_MEDICAL_RECORDS = 5;
  private static final int MAX_RECENT_AI_SCANS = 3;

  private final MedicalRecordRepository medicalRecordRepository;
  private final AIScanRepository aiScanRepository;

  /**
   * Constructs AIAdvisorContextBuilder.
   *
   * @param medicalRecordRepository medical record repository
   * @param aiScanRepository AI scan repository
   */
  public AIAdvisorContextBuilder(
      MedicalRecordRepository medicalRecordRepository, AIScanRepository aiScanRepository) {
    this.medicalRecordRepository = medicalRecordRepository;
    this.aiScanRepository = aiScanRepository;
  }

  /**
   * Assembles a concise, structured animal profile context string.
   *
   * @param animal animal entity
   * @return formatted profile string
   */
  public String buildAnimalContext(Animal animal) {
    if (animal == null) {
      return "Unknown Animal";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("- Name: ").append(animal.getAnimalName() != null ? animal.getAnimalName() : "Unnamed").append("\n");
    sb.append("- Tag Number: ").append(animal.getTagNumber()).append("\n");
    sb.append("- Species: ").append(animal.getSpecies() != null ? animal.getSpecies().name() : "Unknown").append("\n");
    sb.append("- Breed: ").append(animal.getBreed() != null ? animal.getBreed() : "Unknown").append("\n");
    sb.append("- Gender: ").append(animal.getGender() != null ? animal.getGender().name() : "Unknown").append("\n");

    if (animal.getBirthDate() != null) {
      Period age = Period.between(animal.getBirthDate(), LocalDate.now());
      sb.append("- Age: ")
          .append(age.getYears())
          .append(" years, ")
          .append(age.getMonths())
          .append(" months (Born: ")
          .append(animal.getBirthDate())
          .append(")\n");
    }

    return sb.toString().trim();
  }

  /**
   * Assembles recent veterinarian-confirmed Electronic Veterinary Medical Records (EVMR).
   *
   * @param animal animal entity
   * @return formatted confirmed medical history
   */
  public String buildMedicalHistoryContext(Animal animal) {
    if (animal == null || animal.getId() == null) {
      return "No prior electronic veterinary medical records on file.";
    }

    List<MedicalRecord> records =
        medicalRecordRepository.findByAnimalIdOrderByCreatedAtDesc(animal.getId());

    if (records == null || records.isEmpty()) {
      return "No prior veterinarian-confirmed clinical records on file.";
    }

    StringBuilder sb = new StringBuilder();
    int count = Math.min(records.size(), MAX_RECENT_MEDICAL_RECORDS);
    for (int i = 0; i < count; i++) {
      MedicalRecord rec = records.get(i);
      sb.append("[").append(i + 1).append("] Date: ")
          .append(rec.getCreatedAt() != null ? rec.getCreatedAt().toLocalDate() : "N/A")
          .append(" | Veterinarian Diagnosis: ").append(rec.getDiagnosis() != null ? rec.getDiagnosis() : "N/A")
          .append("\n    Symptoms: ").append(rec.getSymptoms() != null ? rec.getSymptoms() : "None reported")
          .append("\n    Clinical Treatment: ").append(rec.getTreatment() != null ? rec.getTreatment() : "N/A")
          .append("\n");
    }

    return sb.toString().trim();
  }

  /**
   * Assembles recent visual AI scans, clearly identifying them as unconfirmed assistive observations.
   *
   * @param animal animal entity
   * @return formatted visual AI observations history
   */
  public String buildPreviousScansContext(Animal animal) {
    if (animal == null) {
      return "No previous AI visual scans on file.";
    }

    List<AIScan> scans = aiScanRepository.findByAnimalOrderByCreatedAtDesc(animal);
    if (scans == null || scans.isEmpty()) {
      return "No previous AI visual scans on file.";
    }

    StringBuilder sb = new StringBuilder();
    int count = Math.min(scans.size(), MAX_RECENT_AI_SCANS);
    for (int i = 0; i < count; i++) {
      AIScan scan = scans.get(i);
      sb.append("[").append(i + 1).append("] (Assistive Observation - Unconfirmed) Date: ")
          .append(scan.getCreatedAt() != null ? scan.getCreatedAt() : "N/A")
          .append(" | Suspected Condition: ").append(scan.getDiagnosis() != null ? scan.getDiagnosis() : "None")
          .append(" | AI Confidence: ").append(scan.getConfidenceScore() != null ? scan.getConfidenceScore() : "N/A")
          .append(" | Status: ").append(scan.getStatus() != null ? scan.getStatus().name() : "N/A")
          .append("\n");
    }

    return sb.toString().trim();
  }

  /**
   * Formats the chronological conversation turns for the advisor prompt.
   *
   * @param messages existing session messages
   * @return formatted conversation dialogue
   */
  public String buildConversationHistoryContext(List<AIAdvisorMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return "Initial conversation turn.";
    }

    StringBuilder sb = new StringBuilder();
    for (AIAdvisorMessage msg : messages) {
      String speaker = msg.getSenderType() == AIAdvisorSenderType.USER ? "Owner" : "Advisor";
      sb.append(speaker).append(": ").append(msg.getContent()).append("\n");
    }

    return sb.toString().trim();
  }

  /**
   * Builds explicit language generation instructions for the AI model based on user preference.
   *
   * @param language ISO language code ('mr', 'hi', 'en')
   * @return language instruction string
   */
  public String buildLanguageInstruction(String language) {
    if ("mr".equalsIgnoreCase(language) || "marathi".equalsIgnoreCase(language)) {
      return "LANGUAGE: MARATHI (मराठी). You MUST generate all user-facing strings ('replyMessage', 'followUpQuestions', condition reasoning, 'userReportedSymptoms', 'keyObservations', 'recommendedNextStep', and 'disclaimer') in clear, natural Marathi. Keep the JSON keys and status/risk enum values in English. The disclaimer must be: 'हे एक एआय-सहाय्यक प्राथमिक मूल्यांकन आहे आणि हे पुष्टी केलेले पशुवैद्यकीय निदान नाही. क्लिनिकल निदान आणि उपचारांसाठी परवानाधारक पशुवैद्यांचा सल्ला घ्या.'";
    } else if ("hi".equalsIgnoreCase(language) || "hindi".equalsIgnoreCase(language)) {
      return "LANGUAGE: HINDI (हिंदी). You MUST generate all user-facing strings ('replyMessage', 'followUpQuestions', condition reasoning, 'userReportedSymptoms', 'keyObservations', 'recommendedNextStep', and 'disclaimer') in clear, natural Hindi. Keep the JSON keys and status/risk enum values in English. The disclaimer must be: 'यह एक एआई-सहायक प्रारंभिक मूल्यांकन है और यह कोई पुष्ट पशु चिकित्सा निदान नहीं है। नैदानिक निदान और उपचार के लिए किसी लाइसेंस प्राप्त पशुचिकित्सक से परामर्श लें।'";
    } else {
      return "LANGUAGE: ENGLISH. Generate all responses in clear, professional English.";
    }
  }
}
