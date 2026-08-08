package app.vetra.ai.rag.retrieval;

import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import java.util.List;

/**
 * Public contract for semantic knowledge retrieval in the veterinary AI platform.
 * Synthesizes grounded literature context and structured citations for prompt injection.
 */
public interface KnowledgeRetriever {

  /**
   * Retrieves grounded context for a clinical query with custom search parameters.
   *
   * @param query natural language or clinical search query
   * @param topK maximum chunks to retrieve
   * @param minSimilarity minimum similarity threshold
   * @param filter metadata filter (species, category, source)
   * @return synthesized context with structured citations
   */
  RetrievedContext retrieveContext(
      String query, int topK, double minSimilarity, SearchFilter filter);

  /**
   * Retrieves grounded context using configured default parameters.
   *
   * @param query clinical query
   * @return synthesized context with structured citations
   */
  RetrievedContext retrieveContext(String query);

  /**
   * Performs raw vector search returning ranked hit records.
   *
   * @param query search query
   * @param topK maximum results
   * @param minSimilarity minimum similarity threshold
   * @param filter metadata filter
   * @return list of search hits
   */
  List<SearchResult> search(
      String query, int topK, double minSimilarity, SearchFilter filter);
}
