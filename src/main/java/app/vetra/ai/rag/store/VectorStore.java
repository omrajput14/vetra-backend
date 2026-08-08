package app.vetra.ai.rag.store;

import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import java.util.List;

/**
 * Provider-independent contract for high-performance vector search and chunk indexing.
 * Raw vectors and indexing internals are completely encapsulated inside implementations.
 */
public interface VectorStore {

  /**
   * Indexes chunks with their corresponding dense vector embeddings.
   *
   * @param chunks domain chunks to index
   * @param vectors dense embedding vectors matching chunks 1:1
   */
  void upsert(List<KnowledgeChunk> chunks, List<float[]> vectors);

  /**
   * Performs semantic vector search with metadata filtering and threshold pruning.
   *
   * @param queryVector dense query vector
   * @param topK maximum results to return
   * @param minSimilarity minimum cosine similarity threshold (0.0 - 1.0)
   * @param filter metadata filter (species, category, source, etc.)
   * @return ranked search hits
   */
  List<SearchResult> search(
      float[] queryVector, int topK, double minSimilarity, SearchFilter filter);

  /**
   * Deletes all indexed chunks belonging to a document.
   *
   * @param documentId document identifier
   */
  void delete(String documentId);

  /**
   * Checks if a document exists in the store.
   *
   * @param documentId document identifier
   * @return true if indexed chunks exist for this document
   */
  boolean exists(String documentId);

  /**
   * Returns total count of indexed chunks.
   *
   * @return chunk count
   */
  long count();

  /** Purges all chunks from the vector store. */
  void clear();
}
