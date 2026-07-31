package app.vetra.disease.repository;

import app.vetra.disease.entity.DiagnosisStatus;
import app.vetra.disease.entity.DiseaseReport;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for DiseaseReport entities with PostGIS spatial queries.
 */
@Repository
public interface DiseaseReportRepository extends JpaRepository<DiseaseReport, UUID> {

  /** Finds all disease reports for an animal ordered by created date descending. */
  List<DiseaseReport> findByAnimalIdOrderByCreatedAtDesc(UUID animalId);

  /** Finds all disease reports submitted by a specific user with pagination. */
  Page<DiseaseReport> findByReportedById(UUID reportedById, Pageable pageable);

  /** Finds disease reports matching disease name and diagnosis status. */
  List<DiseaseReport> findByDiseaseNameIgnoreCaseAndDiagnosisStatus(String diseaseName, DiagnosisStatus diagnosisStatus);

  /** Finds confirmed reports for a specific disease name. */
  List<DiseaseReport> findByDiseaseNameIgnoreCaseAndDiagnosisStatusOrderByCreatedAtDesc(
      String diseaseName, DiagnosisStatus diagnosisStatus);

  /**
   * Spatial bounding box search for disease reports within latitude and longitude bounds.
   */
  @Query("""
      SELECT r FROM DiseaseReport r
      WHERE r.latitude BETWEEN :minLat AND :maxLat
        AND r.longitude BETWEEN :minLon AND :maxLon
      ORDER BY r.createdAt DESC
      """)
  List<DiseaseReport> findWithinBoundingBox(
      @Param("minLat") Double minLat,
      @Param("maxLat") Double maxLat,
      @Param("minLon") Double minLon,
      @Param("maxLon") Double maxLon);
}
