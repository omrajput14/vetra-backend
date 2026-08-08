package app.vetra.ai.workflow.clinical.model.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SensorObservationTest {

  @Test
  void testSensorObservationDefaults() {
    SensorObservation sensor = new SensorObservation(null, null, 100.0, null, null, null);

    assertNotNull(sensor);
    assertEquals("SENSOR-UNKNOWN", sensor.sensorId());
    assertEquals("GENERAL", sensor.sensorType());
    assertEquals(100.0, sensor.reading());
    assertEquals("", sensor.unit());
    assertNotNull(sensor.timestamp());
    assertEquals("OK", sensor.status());
  }

  @Test
  void testSensorObservationCustomValues() {
    Instant now = Instant.now();
    SensorObservation sensor =
        new SensorObservation(" RUMINATION-01 ", " rumination ", 85.0, " min/day ", now, " LOW ");

    assertEquals("RUMINATION-01", sensor.sensorId());
    assertEquals("RUMINATION", sensor.sensorType());
    assertEquals(85.0, sensor.reading());
    assertEquals("min/day", sensor.unit());
    assertEquals(now, sensor.timestamp());
    assertEquals("LOW", sensor.status());
  }
}
