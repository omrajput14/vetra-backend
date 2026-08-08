package app.vetra.ai.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.KnowledgeDocument;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

  private DocumentChunker chunker;
  private KnowledgeDocument testDoc;

  @BeforeEach
  void setUp() {
    chunker = new DocumentChunker();
    testDoc =
        KnowledgeDocument.of(
            "doc-1",
            "Bovine Mastitis Guidelines",
            "FAO Veterinary Division",
            "INFECTIOUS",
            "Sample content",
            "TEXT",
            Map.of("species", "CATTLE", "source", "FAO"));
  }

  @Test
  void testChunk_shortTextSingleChunk() {
    String text = "Bovine mastitis is an inflammatory condition of the mammary gland.";
    List<KnowledgeChunk> chunks = chunker.chunk(testDoc, text, testDoc.metadata());

    assertEquals(1, chunks.size());
    KnowledgeChunk chunk = chunks.get(0);
    assertEquals("doc-1-chunk-0", chunk.id());
    assertEquals("Bovine Mastitis Guidelines", chunk.documentTitle());
    assertEquals("CATTLE", chunk.metadata().get("species"));
    assertEquals(text, chunk.content());
    assertTrue(chunk.tokenCount() > 0);
  }

  @Test
  void testChunk_longTextMultipleChunksWithOverlap() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 20; i++) {
      sb.append("Section ").append(i).append(": Clinical signs include heat, swelling, and abnormal milk secretions. ");
    }
    String longText = sb.toString();

    List<KnowledgeChunk> chunks = chunker.chunk(testDoc, longText, testDoc.metadata(), 200, 40);

    assertTrue(chunks.size() > 1, "Expected multiple chunks for long text");
    for (int i = 0; i < chunks.size(); i++) {
      KnowledgeChunk chunk = chunks.get(i);
      assertEquals(i, chunk.chunkIndex());
      assertFalse(chunk.content().isEmpty());
    }
  }

  @Test
  void testChunk_emptyOrNullText() {
    assertTrue(chunker.chunk(testDoc, "", testDoc.metadata()).isEmpty());
    assertTrue(chunker.chunk(testDoc, null, testDoc.metadata()).isEmpty());
    assertTrue(chunker.chunk(testDoc, "   ", testDoc.metadata()).isEmpty());
  }
}
