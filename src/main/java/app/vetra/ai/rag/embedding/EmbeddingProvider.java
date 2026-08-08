package app.vetra.ai.rag.embedding;

import java.util.List;

/**
 * Provider-agnostic contract for generating dense vector embeddings from clinical text.
 * Implementations may include local deterministic encoders, Gemini Embeddings, OpenAI, etc.
 */
public interface EmbeddingProvider {

  /**
   * Generates a normalized dense vector embedding for a single text.
   *
   * @param text input clinical text
   * @return normalized float vector
   */
  float[] generateEmbedding(String text);

  /**
   * Generates batch embeddings for multiple text chunks.
   *
   * @param texts list of input texts
   * @return list of normalized float vectors
   */
  List<float[]> generateEmbeddings(List<String> texts);

  /**
   * Returns the vector dimension size (e.g. 128, 768, 1536).
   *
   * @return embedding dimension
   */
  int embeddingDimension();

  /**
   * Returns the provider identifier (e.g. "local-deterministic", "gemini", "openai").
   *
   * @return provider name
   */
  String providerName();
}
