package app.vetra.ai.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmbeddingProviderTest {

  private DeterministicEmbeddingProvider provider;

  @BeforeEach
  void setUp() {
    provider = new DeterministicEmbeddingProvider(64);
  }

  @Test
  void testGenerateEmbedding_dimensionAndNormalization() {
    float[] vector = provider.generateEmbedding("Bovine mastitis infection in cattle");

    assertNotNull(vector);
    assertEquals(64, vector.length);

    // Verify L2 norm is ~1.0
    float sumSq = 0.0f;
    for (float v : vector) {
      sumSq += v * v;
    }
    assertTrue(Math.abs(1.0f - Math.sqrt(sumSq)) < 0.01f, "Vector should be L2 normalized");
  }

  @Test
  void testGenerateEmbedding_semanticSimilarity() {
    float[] v1 = provider.generateEmbedding("Bovine mastitis diagnosis");
    float[] v2 = provider.generateEmbedding("Bovine mastitis clinical signs");
    float[] v3 = provider.generateEmbedding("Avian influenza virus poultry");

    double sim12 = cosineSimilarity(v1, v2);
    double sim13 = cosineSimilarity(v1, v3);

    assertTrue(sim12 > sim13, "Related veterinary terms should produce higher cosine similarity");
  }

  @Test
  void testBatchEmbeddings() {
    List<float[]> list = provider.generateEmbeddings(List.of("Doc 1", "Doc 2"));
    assertEquals(2, list.size());
    assertEquals(64, list.get(0).length);
    assertEquals(64, list.get(1).length);
  }

  private double cosineSimilarity(float[] v1, float[] v2) {
    double dot = 0.0;
    for (int i = 0; i < v1.length; i++) {
      dot += v1[i] * v2[i];
    }
    return dot;
  }
}
