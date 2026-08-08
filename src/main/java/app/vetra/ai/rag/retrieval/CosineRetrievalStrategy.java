package app.vetra.ai.rag.retrieval;

import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import app.vetra.ai.rag.store.VectorStore;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Standard dense vector cosine similarity retrieval strategy.
 * Delegates search and metadata filtering directly to the underlying {@link VectorStore}.
 */
@Component
public class CosineRetrievalStrategy implements RetrievalStrategy {

  public static final String STRATEGY_NAME = "cosine";

  private final VectorStore vectorStore;

  /**
   * Constructs CosineRetrievalStrategy.
   *
   * @param vectorStore underlying vector store
   */
  public CosineRetrievalStrategy(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  @Override
  public List<SearchResult> search(
      String queryText,
      float[] queryVector,
      int topK,
      double minSimilarity,
      SearchFilter filter) {
    return vectorStore.search(queryVector, topK, minSimilarity, filter);
  }

  @Override
  public String strategyName() {
    return STRATEGY_NAME;
  }
}
