package app.vetra.ai.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.rag.embedding.DeterministicEmbeddingProvider;
import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.KnowledgeDocument;
import app.vetra.ai.rag.store.InMemoryVectorStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentIngestionServiceTest {

  private DocumentIngestionService ingestionService;
  private InMemoryVectorStore vectorStore;

  @BeforeEach
  void setUp() {
    DocumentParser parser = new DocumentParser();
    DocumentChunker chunker = new DocumentChunker();
    DeterministicEmbeddingProvider embeddingProvider = new DeterministicEmbeddingProvider(64);
    vectorStore = new InMemoryVectorStore();
    AIMetricsCollector metricsCollector = new AIMetricsCollector(new SimpleMeterRegistry());

    ingestionService =
        new DocumentIngestionService(
            parser, chunker, embeddingProvider, vectorStore, metricsCollector);
  }

  @Test
  void testIngestDocument() {
    KnowledgeDocument doc =
        KnowledgeDocument.of(
            "doc-vet-1",
            "Foot and Mouth Disease Handbook",
            "WOAH",
            "INFECTIOUS",
            "Foot and mouth disease is a severe, highly contagious viral disease of livestock. "
                + "It affects cattle, swine, sheep, goats and other cloven-hoofed ruminants.",
            "TEXT",
            Map.of("species", "CATTLE", "source", "WOAH"));

    List<KnowledgeChunk> chunks = ingestionService.ingestDocument(doc);

    assertEquals(1, chunks.size());
    assertTrue(vectorStore.exists("doc-vet-1"));
    assertEquals(1, vectorStore.count());

    ingestionService.deleteDocument("doc-vet-1");
    assertEquals(0, vectorStore.count());
  }

  @Test
  void testIngestBatch() {
    KnowledgeDocument doc1 =
        KnowledgeDocument.of(
            "doc-1", "Title 1", "Auth 1", "CAT1", "Clinical content 1", "TEXT", Map.of());
    KnowledgeDocument doc2 =
        KnowledgeDocument.of(
            "doc-2", "Title 2", "Auth 2", "CAT2", "Clinical content 2", "TEXT", Map.of());

    int chunksIndexed = ingestionService.ingestBatch(List.of(doc1, doc2));
    assertEquals(2, chunksIndexed);
    assertEquals(2, vectorStore.count());
  }
}
