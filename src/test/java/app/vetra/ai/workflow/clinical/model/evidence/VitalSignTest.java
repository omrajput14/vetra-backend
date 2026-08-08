package app.vetra.ai.workflow.clinical.model.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class VitalSignTest {

  @Test
  void testVitalSignDefaults() {
    VitalSign vital = new VitalSign(null, 38.5, null, null, null);

    assertNotNull(vital);
    assertEquals("UNKNOWN", vital.measurementType());
    assertEquals(38.5, vital.value());
    assertEquals("", vital.unit());
    assertEquals(AbnormalityStatus.UNKNOWN, vital.status());
    assertNotNull(vital.timestamp());
  }

  @Test
  void testVitalSignCustomValues() {
    Instant now = Instant.now();
    VitalSign vital = new VitalSign(" temperature ", 40.5, " C ", AbnormalityStatus.CRITICAL, now);

    assertEquals("TEMPERATURE", vital.measurementType());
    assertEquals(40.5, vital.value());
    assertEquals("C", vital.unit());
    assertEquals(AbnormalityStatus.CRITICAL, vital.status());
    assertEquals(now, vital.timestamp());
  }
}
