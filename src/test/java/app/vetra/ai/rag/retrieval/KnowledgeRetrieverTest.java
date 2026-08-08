package app.vetra.ai.rag.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.config.RagProperties;
import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.rag.embedding.DeterministicEmbeddingProvider;
import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.RetrievedContext;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.store.InMemoryVectorStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeRetrieverTest {

  private DefaultKnowledgeRetriever retriever;
  private InMemoryVectorStore vectorStore;
  private DeterministicEmbeddingProvider embeddingProvider;

  @BeforeEach
  void setUp() {
    embeddingProvider = new DeterministicEmbeddingProvider(64);
    vectorStore = new InMemoryVectorStore();
    CosineRetrievalStrategy strategy = new CosineRetrievalStrategy(vectorStore);
    RagProperties properties = new RagProperties();
    AIMetricsCollector metrics = new AIMetricsCollector(new SimpleMeterRegistry());

    retriever = new DefaultKnowledgeRetriever(embeddingProvider, strategy, properties, metrics, null);

    // Index sample clinical knowledge
    KnowledgeChunk chunk =
        new KnowledgeChunk(
            "chunk-101",
            "doc-fmd",
            "FMD Guidelines 2026",
            0,
            "Foot and mouth disease causes vesicular lesions on tongue, lips, and hooves.",
            35,
            Map.of("species", "CATTLE", "category", "VIRAL", "source", "FAO_VET"));

    float[] vec = embeddingProvider.generateEmbedding("Foot and mouth disease vesicular lesions hooves");
    vectorStore.upsert(List.of(chunk), List.of(vec));
  }

  @Test
  void testRetrieveContext_matchingQueryReturnsFormattedContextAndCitations() {
    RetrievedContext context = retriever.retrieveContext("Foot and mouth lesions", 3, 0.40, SearchFilter.empty());

    assertTrue(context.hasContext());
    assertEquals(1, context.totalChunks());
    assertEquals(1, context.citations().size());
    assertEquals("FMD Guidelines 2026", context.citations().get(0).documentTitle());
    assertEquals("FAO_VET", context.citations().get(0).source());
    assertTrue(context.contextText().contains("Foot and mouth disease causes"));
    assertTrue(context.avgSimilarityScore() > 0.40);
  }

  @Test
  void testRetrieveContext_emptyOnNoMatchOrEmptyQuery() {
    RetrievedContext emptyContext = retriever.retrieveContext("");
    assertFalse(emptyContext.hasContext());
    assertEquals(0, emptyContext.totalChunks());

    RetrievedContext nullContext = retriever.retrieveContext(null);
    assertFalse(nullContext.hasContext());
  }
}
