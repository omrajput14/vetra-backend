package app.vetra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Enterprise configuration properties for the Veterinary Knowledge Retrieval (RAG) platform.
 */
@ConfigurationProperties(prefix = "vetra.ai.rag")
public class RagProperties {

  private boolean enabled = true;
  private int topK = 3;
  private double similarityThreshold = 0.65;
  private int chunkSize = 500;
  private int overlap = 50;
  private String embeddingProvider = "local";
  private String vectorStoreProvider = "in-memory";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getTopK() {
    return topK;
  }

  public void setTopK(int topK) {
    this.topK = topK;
  }

  public double getSimilarityThreshold() {
    return similarityThreshold;
  }

  public void setSimilarityThreshold(double similarityThreshold) {
    this.similarityThreshold = similarityThreshold;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getOverlap() {
    return overlap;
  }

  public void setOverlap(int overlap) {
    this.overlap = overlap;
  }

  public String getEmbeddingProvider() {
    return embeddingProvider;
  }

  public void setEmbeddingProvider(String embeddingProvider) {
    this.embeddingProvider = embeddingProvider;
  }

  public String getVectorStoreProvider() {
    return vectorStoreProvider;
  }

  public void setVectorStoreProvider(String vectorStoreProvider) {
    this.vectorStoreProvider = vectorStoreProvider;
  }
}
