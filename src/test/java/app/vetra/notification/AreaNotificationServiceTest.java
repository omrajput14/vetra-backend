package app.vetra.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.notification.service.AreaNotificationService;
import org.junit.jupiter.api.Test;

/** Radius check used to pick who gets containment / escalation pushes. */
class AreaNotificationServiceTest {

  // Wagholi farm (Pune) and Baramati clinic are about 80 km apart.
  private static final double FARM_LAT = 18.5793;
  private static final double FARM_LNG = 73.9826;

  @Test
  void pointsInsideAndOutsideTheRadius() {
    assertTrue(AreaNotificationService.inside(18.5601, 73.9502, FARM_LAT, FARM_LNG, 15));
    assertFalse(AreaNotificationService.inside(18.1512, 74.5781, FARM_LAT, FARM_LNG, 50));
    assertTrue(AreaNotificationService.inside(18.1512, 74.5781, FARM_LAT, FARM_LNG, 100));
  }

  @Test
  void noLocationIsNeverInside() {
    assertFalse(AreaNotificationService.inside(null, 73.95, FARM_LAT, FARM_LNG, 500));
    assertFalse(AreaNotificationService.inside(18.56, null, FARM_LAT, FARM_LNG, 500));
  }
}
