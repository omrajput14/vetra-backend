package app.vetra.ai.rag.retrieval;

import app.vetra.ai.config.RagProperties;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.observability.AIObservationConvention;
import app.vetra.ai.rag.embedding.EmbeddingProvider;
import app.vetra.ai.rag.model.Citation;
import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link KnowledgeRetriever}. Coordinates query embedding, candidate
 * search execution via {@link RetrievalStrategy}, structured citation assembly, and RAG observability.
 */
@Service
public class DefaultKnowledgeRetriever implements KnowledgeRetriever {

  private static final Logger log = LoggerFactory.getLogger(DefaultKnowledgeRetriever.class);

  private final EmbeddingProvider embeddingProvider;
  private final RetrievalStrategy retrievalStrategy;
  private final RagProperties ragProperties;
  private final AIMetricsCollector metricsCollector;
  private final AIObservationConvention observationConvention;

  /**
   * Constructs DefaultKnowledgeRetriever.
   *
   * @param embeddingProvider embedding provider
   * @param retrievalStrategy candidate search strategy
   * @param ragProperties configuration properties (optional)
   * @param metricsCollector operational metrics collector (optional)
   * @param observationConvention tracer observation convention (optional)
   */
  public DefaultKnowledgeRetriever(
      EmbeddingProvider embeddingProvider,
      RetrievalStrategy retrievalStrategy,
      @Autowired(required = false) RagProperties ragProperties,
      @Autowired(required = false) AIMetricsCollector metricsCollector,
      @Autowired(required = false) AIObservationConvention observationConvention) {
    this.embeddingProvider = embeddingProvider;
    this.retrievalStrategy = retrievalStrategy;
    this.ragProperties = ragProperties != null ? ragProperties : new RagProperties();
    this.metricsCollector = metricsCollector;
    this.observationConvention = observationConvention;
  }

  @Override
  public RetrievedContext retrieveContext(String query) {
    int topK = ragProperties.getTopK() > 0 ? ragProperties.getTopK() : 3;
    double minSimilarity =
        ragProperties.getSimilarityThreshold() > 0 ? ragProperties.getSimilarityThreshold() : 0.65;
    return retrieveContext(query, topK, minSimilarity, SearchFilter.empty());
  }

  @Override
  public RetrievedContext retrieveContext(
      String query, int topK, double minSimilarity, SearchFilter filter) {

    if (!ragProperties.isEnabled() || query == null || query.isBlank()) {
      return RetrievedContext.empty();
    }

    long startNanos = System.nanoTime();
    if (observationConvention != null) {
      observationConvention.recordSpanEvent("rag.retrieval.started");
    }

    try {
      List<SearchResult> searchHits = search(query, topK, minSimilarity, filter);
      long durationNanos = System.nanoTime() - startNanos;

      if (searchHits.isEmpty()) {
        log.info("RAG search returned 0 matching chunks for query='{}'", query);
        if (metricsCollector != null) {
          metricsCollector.recordRagQuery(0, 0.0, durationNanos);
        }
        return RetrievedContext.empty();
      }

      StringBuilder contextBuilder = new StringBuilder();
      List<Citation> citations = new ArrayList<>(searchHits.size());
      int totalTokens = 0;
      double totalSimilarity = 0.0;

      for (int i = 0; i < searchHits.size(); i++) {
        SearchResult hit = searchHits.get(i);
        KnowledgeChunk chunk = hit.chunk();
        double score = hit.similarityScore();
        totalSimilarity += score;
        totalTokens += chunk.tokenCount();

        String source =
            chunk.metadata() != null
                ? chunk.metadata().getOrDefault("source", "VETRA_LITERATURE")
                : "VETRA_LITERATURE";

        citations.add(new Citation(chunk.documentTitle(), chunk.id(), source, score));

        if (i > 0) {
          contextBuilder.append("\n\n");
        }
        contextBuilder
            .append("[Source #")
            .append(i + 1)
            .append(": ")
            .append(chunk.documentTitle())
            .append(" (")
            .append(source)
            .append(")]\n")
            .append(chunk.content());
      }

      double avgSimilarity = totalSimilarity / searchHits.size();

      if (metricsCollector != null) {
        metricsCollector.recordRagQuery(searchHits.size(), avgSimilarity, durationNanos);
      }

      if (observationConvention != null) {
        observationConvention.recordSpanEvent("rag.retrieval.completed");
      }

      log.info(
          "RAG retrieved {} chunks (avgScore={}) in {}ms for query='{}'",
          searchHits.size(),
          String.format("%.3f", avgSimilarity),
          durationNanos / 1_000_000,
          query);

      return new RetrievedContext(
          contextBuilder.toString(), citations, searchHits.size(), totalTokens, avgSimilarity);
    } catch (Exception ex) {
      log.error("RAG retrieval failed for query='{}': {}", query, ex.getMessage());
      return RetrievedContext.empty();
    }
  }

  @Override
  public List<SearchResult> search(
      String query, int topK, double minSimilarity, SearchFilter filter) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    float[] queryVector = embeddingProvider.generateEmbedding(query);
    return retrievalStrategy.search(query, queryVector, topK, minSimilarity, filter);
  }
}
