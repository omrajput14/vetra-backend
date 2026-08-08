package app.vetra.ai.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Standard normalized dense embedding provider producing deterministic vectors using term frequency
 * and character n-gram projections. Suitable for local testing and unit execution without external
 * API network overhead.
 */
@Component
public class DeterministicEmbeddingProvider implements EmbeddingProvider {

  public static final int DEFAULT_DIMENSION = 128;
  public static final String PROVIDER_NAME = "local-deterministic";

  private final int dimension;

  /** Constructs DeterministicEmbeddingProvider with default vector dimension (128). */
  public DeterministicEmbeddingProvider() {
    this(DEFAULT_DIMENSION);
  }

  /**
   * Constructs DeterministicEmbeddingProvider with custom vector dimension.
   *
   * @param dimension dense vector dimension size
   */
  public DeterministicEmbeddingProvider(int dimension) {
    this.dimension = dimension > 0 ? dimension : DEFAULT_DIMENSION;
  }

  @Override
  public float[] generateEmbedding(String text) {
    float[] vector = new float[dimension];
    if (text == null || text.isBlank()) {
      return vector;
    }

    String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
    String[] tokens = normalized.split("\\s+");

    for (String token : tokens) {
      if (token.isBlank()) {
        continue;
      }
      int hash = Math.abs(token.hashCode());
      int idx = hash % dimension;
      vector[idx] += 1.0f;

      // Add n-gram sub-features for semantic overlap
      if (token.length() >= 3) {
        for (int i = 0; i <= token.length() - 3; i++) {
          String tri = token.substring(i, i + 3);
          int triIdx = Math.abs(tri.hashCode()) % dimension;
          vector[triIdx] += 0.5f;
        }
      }
    }

    // L2 Normalize
    float sumSq = 0.0f;
    for (float v : vector) {
      sumSq += v * v;
    }

    if (sumSq > 0.0f) {
      float norm = (float) Math.sqrt(sumSq);
      for (int i = 0; i < vector.length; i++) {
        vector[i] = vector[i] / norm;
      }
    }

    return vector;
  }

  @Override
  public List<float[]> generateEmbeddings(List<String> texts) {
    if (texts == null) {
      return List.of();
    }
    List<float[]> result = new ArrayList<>(texts.size());
    for (String text : texts) {
      result.add(generateEmbedding(text));
    }
    return result;
  }

  @Override
  public int embeddingDimension() {
    return dimension;
  }

  @Override
  public String providerName() {
    return PROVIDER_NAME;
  }
}
