package app.vetra.ai.workflow.clinical.model.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LaboratoryResultTest {

  @Test
  void testLaboratoryResultDefaults() {
    LaboratoryResult lab = new LaboratoryResult(null, null, null, null, null, null, null);

    assertNotNull(lab);
    assertEquals("Unspecified Lab Test", lab.testName());
    assertEquals("N/A", lab.value());
    assertEquals("", lab.unit());
    assertEquals("N/A", lab.referenceRange());
    assertEquals(AbnormalityStatus.UNKNOWN, lab.status());
    assertNotNull(lab.timestamp());
    assertEquals("", lab.notes());
  }

  @Test
  void testLaboratoryResultCustomValues() {
    Instant now = Instant.now();
    LaboratoryResult lab =
        new LaboratoryResult(
            " Blood Glucose ",
            " 180 ",
            " mg/dL ",
            " 70 - 120 ",
            AbnormalityStatus.HIGH,
            now,
            " Fasting ");

    assertEquals("Blood Glucose", lab.testName());
    assertEquals("180", lab.value());
    assertEquals("mg/dL", lab.unit());
    assertEquals("70 - 120", lab.referenceRange());
    assertEquals(AbnormalityStatus.HIGH, lab.status());
    assertEquals(now, lab.timestamp());
    assertEquals("Fasting", lab.notes());
  }
}
