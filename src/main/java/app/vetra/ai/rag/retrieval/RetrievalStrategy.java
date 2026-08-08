package app.vetra.ai.rag.retrieval;

import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import java.util.List;

/**
 * Strategy contract for veterinary knowledge retrieval algorithms.
 * Allows pluggable search strategies (Cosine similarity, Hybrid dense/sparse, BM25, Reciprocal Rank Fusion)
 * without modifying the core retriever.
 */
public interface RetrievalStrategy {

  /**
   * Executes candidate search and ranking over indexed knowledge vectors.
   *
   * @param queryText natural language query
   * @param queryVector dense vector embedding of the query
   * @param topK maximum candidate hits to return
   * @param minSimilarity minimum similarity threshold (0.0 to 1.0)
   * @param filter metadata filter (species, category, source, etc.)
   * @return ranked search results
   */
  List<SearchResult> search(
      String queryText,
      float[] queryVector,
      int topK,
      double minSimilarity,
      SearchFilter filter);

  /**
   * Returns the strategy name identifier (e.g. "cosine", "hybrid", "bm25-rrf").
   *
   * @return strategy identifier
   */
  String strategyName();
}
