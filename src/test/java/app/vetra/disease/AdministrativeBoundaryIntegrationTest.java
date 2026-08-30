package app.vetra.disease;

import static org.assertj.core.api.Assertions.assertThat;

import app.vetra.disease.controller.AdministrativeBoundaryController;
import app.vetra.disease.geo.AdministrativeBoundaryService;
import app.vetra.disease.geo.GeoJsonFeatureCollection;
import app.vetra.infrastructure.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AdministrativeBoundaryIntegrationTest {

  @Autowired
  private AdministrativeBoundaryService boundaryService;

  @Autowired
  private AdministrativeBoundaryController boundaryController;

  @Test
  @DisplayName("Verifies actual feature counts and property integrity in GeoJSON datasets")
  void shouldLoadAndValidateAllAdministrativeBoundaries() {
    GeoJsonFeatureCollection stateCollection = boundaryService.getBoundaries("STATE", null, "Maharashtra");
    assertThat(stateCollection).isNotNull();
    assertThat(stateCollection.features()).hasSize(1);
    GeoJsonFeatureCollection.GeoJsonFeature stateFeature = stateCollection.features().get(0);
    assertThat(stateFeature.type()).isEqualTo("Feature");
    assertThat(stateFeature.geometry().type()).isIn("Polygon", "MultiPolygon");
    assertThat(stateFeature.properties().get("administrativeLevel")).isEqualTo("STATE");
    assertThat(stateFeature.properties().get("state")).isEqualTo("Maharashtra");

    GeoJsonFeatureCollection districtCollection = boundaryService.getBoundaries("DISTRICT", null, "Maharashtra");
    assertThat(districtCollection).isNotNull();
    assertThat(districtCollection.features()).hasSize(36);
    for (GeoJsonFeatureCollection.GeoJsonFeature f : districtCollection.features()) {
      assertThat(f.type()).isEqualTo("Feature");
      assertThat(f.geometry().type()).isIn("Polygon", "MultiPolygon");
      assertThat(f.properties().get("administrativeLevel")).isEqualTo("DISTRICT");
      assertThat(f.properties().get("name")).isNotNull();
      assertThat(f.properties().get("state")).isEqualTo("Maharashtra");
    }

    GeoJsonFeatureCollection talukaCollection = boundaryService.getBoundaries("TALUKA", null, "Maharashtra");
    assertThat(talukaCollection).isNotNull();
    assertThat(talukaCollection.features()).hasSize(357);
    for (GeoJsonFeatureCollection.GeoJsonFeature f : talukaCollection.features()) {
      assertThat(f.type()).isEqualTo("Feature");
      assertThat(f.geometry().type()).isIn("Polygon", "MultiPolygon");
      assertThat(f.properties().get("administrativeLevel")).isEqualTo("TALUKA");
      assertThat(f.properties().get("name")).isNotNull();
      assertThat(f.properties().get("district")).isNotNull();
      assertThat(f.properties().get("state")).isEqualTo("Maharashtra");
    }

    GeoJsonFeatureCollection allCollection = boundaryService.getBoundaries("ALL", null, "Maharashtra");
    assertThat(allCollection).isNotNull();
    assertThat(allCollection.features()).hasSize(394); // 1 state + 36 districts + 357 talukas
  }

  @Test
  @DisplayName("Verifies dynamic extraction of available districts directly from GeoJSON properties")
  void shouldReturnDynamicDistrictsList() {
    List<String> districts = boundaryService.getAvailableDistricts();
    assertThat(districts).hasSize(36);
    assertThat(districts).contains("Pune", "Satara", "Kolhapur", "Nagpur", "Nashik", "Ahmednagar", "Thane");
  }

  @Test
  @DisplayName("Verifies district filtering for taluka polygons")
  void shouldFilterTalukasByDistrict() {
    GeoJsonFeatureCollection puneTalukas = boundaryService.getBoundaries("TALUKA", "Pune", "Maharashtra");
    assertThat(puneTalukas).isNotNull();
    assertThat(puneTalukas.features()).isNotEmpty();
    for (GeoJsonFeatureCollection.GeoJsonFeature f : puneTalukas.features()) {
      Map<String, Object> props = f.properties();
      assertThat(props.get("district").toString()).isEqualToIgnoringCase("Pune");
    }
  }

  @Test
  @DisplayName("Verifies REST API endpoint GET /api/v1/geo/boundaries")
  void shouldReturnBoundariesViaRestApi() {
    ApiResponse<GeoJsonFeatureCollection> response = boundaryController.getBoundaries("DISTRICT", null, "Maharashtra");
    assertThat(response.success()).isTrue();
    assertThat(response.data().features()).hasSize(36);
  }

  @Test
  @DisplayName("Verifies REST API endpoint GET /api/v1/geo/districts")
  void shouldReturnDistrictsViaRestApi() {
    ApiResponse<List<String>> response = boundaryController.getAvailableDistricts();
    assertThat(response.success()).isTrue();
    assertThat(response.data()).hasSize(36);
    assertThat(response.data()).contains("Pune", "Mumbai", "Nagpur");
  }
}
