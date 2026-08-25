package app.vetra.animal.service;

import app.vetra.animal.dto.AnimalHealthRecordDto;
import app.vetra.animal.dto.AnimalHealthStatusDto;
import app.vetra.animal.dto.CreateHealthRecordRequest;
import java.util.List;
import java.util.UUID;

/** Service interface for managing animal digital medical passport and lifetime health timeline. */
public interface AnimalHealthRecordService {

  /** Creates a new timeline health record with caller authorization check. */
  AnimalHealthRecordDto createRecord(
      String userIdentifier, UUID animalId, CreateHealthRecordRequest request);

  /** Appends a timeline record directly from internal subsystems (AI Advisor, scans, IoT). */
  AnimalHealthRecordDto createInternalRecord(UUID animalId, CreateHealthRecordRequest request);

  /** Retrieves the chronological health timeline for an animal (newest first). */
  List<AnimalHealthRecordDto> getAnimalTimeline(String userIdentifier, UUID animalId);

  /** Computes the dynamic latest health status from recent timeline events. */
  AnimalHealthStatusDto getLatestHealthStatus(String userIdentifier, UUID animalId);
}
