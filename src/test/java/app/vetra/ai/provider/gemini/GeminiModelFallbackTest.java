package app.vetra.ai.provider.gemini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Gemini model fallback: busy (503) or rate-limited (429) models are skipped, other errors are not. */
class GeminiModelFallbackTest {

  private static WebClientResponseException http(int code) {
    return WebClientResponseException.create(code, "status " + code, null, null, null);
  }

  @Test
  void busyModelFallsBackToTheNextOne() {
    List<String> tried = new ArrayList<>();
    String used = GeminiAIProvider.firstAvailableModel(
        List.of("gemini-3.5-flash", "gemini-2.5-flash", "gemini-3.5-flash-lite"),
        model -> {
          tried.add(model);
          if (model.equals("gemini-3.5-flash")) {
            throw http(503);
          }
          return model;
        });
    assertEquals("gemini-2.5-flash", used);
    assertEquals(List.of("gemini-3.5-flash", "gemini-2.5-flash"), tried);
  }

  @Test
  void rateLimitAlsoFallsBack() {
    String used = GeminiAIProvider.firstAvailableModel(
        List.of("a", "b"), model -> {
          if (model.equals("a")) {
            throw http(429);
          }
          return model;
        });
    assertEquals("b", used);
  }

  @Test
  void otherErrorsDoNotTryOtherModels() {
    List<String> tried = new ArrayList<>();
    assertThrows(WebClientResponseException.class, () -> GeminiAIProvider.firstAvailableModel(
        List.of("a", "b"), model -> {
          tried.add(model);
          throw http(400);
        }));
    assertEquals(List.of("a"), tried);
  }

  @Test
  void allModelsBusyThrowsTheLastError() {
    WebClientResponseException ex = assertThrows(WebClientResponseException.class,
        () -> GeminiAIProvider.firstAvailableModel(List.of("a", "b"), model -> {
          throw http(503);
        }));
    assertEquals(503, ex.getStatusCode().value());
  }
}
