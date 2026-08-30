package app.vetra.developer.service;

import app.vetra.animal.repository.AnimalHealthRecordRepository;
import app.vetra.animal.repository.AnimalRepository;
import app.vetra.appointment.repository.AppointmentChatMessageRepository;
import app.vetra.appointment.repository.AppointmentRepository;
import app.vetra.medicalrecord.repository.MedicalRecordRepository;
import org.springframework.stereotype.Component;

/** Holder component encapsulating clinical and appointment repositories. */
@Component
public class ClinicalDataHolder {

  private final AnimalRepository animalRepository;
  private final AnimalHealthRecordRepository animalHealthRecordRepository;
  private final AppointmentRepository appointmentRepository;
  private final AppointmentChatMessageRepository chatMessageRepository;
  private final MedicalRecordRepository medicalRecordRepository;

  public ClinicalDataHolder(
      AnimalRepository animalRepository,
      AnimalHealthRecordRepository animalHealthRecordRepository,
      AppointmentRepository appointmentRepository,
      AppointmentChatMessageRepository chatMessageRepository,
      MedicalRecordRepository medicalRecordRepository) {
    this.animalRepository = animalRepository;
    this.animalHealthRecordRepository = animalHealthRecordRepository;
    this.appointmentRepository = appointmentRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.medicalRecordRepository = medicalRecordRepository;
  }

  public AnimalRepository animalRepo() {
    return animalRepository;
  }

  public AnimalHealthRecordRepository healthRecordRepo() {
    return animalHealthRecordRepository;
  }

  public AppointmentRepository appointmentRepo() {
    return appointmentRepository;
  }

  public AppointmentChatMessageRepository chatRepo() {
    return chatMessageRepository;
  }

  public MedicalRecordRepository medicalRecordRepo() {
    return medicalRecordRepository;
  }
}
