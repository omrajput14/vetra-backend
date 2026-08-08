package app.vetra.ai.rag.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryVectorStoreTest {

  private InMemoryVectorStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryVectorStore();
  }

  @Test
  void testUpsertAndSearch_rankingAndFiltering() {
    KnowledgeChunk chunk1 =
        new KnowledgeChunk(
            "chunk-1",
            "doc-1",
            "Bovine Health",
            0,
            "Mastitis treatment in cattle",
            50,
            Map.of("species", "CATTLE", "category", "INFECTIOUS", "source", "FAO"));

    KnowledgeChunk chunk2 =
        new KnowledgeChunk(
            "chunk-2",
            "doc-2",
            "Avian Protocol",
            0,
            "Avian flu prevention in poultry",
            40,
            Map.of("species", "POULTRY", "category", "VIRAL", "source", "WOAH"));

    float[] vec1 = new float[] {1.0f, 0.0f, 0.0f};
    float[] vec2 = new float[] {0.0f, 1.0f, 0.0f};

    store.upsert(List.of(chunk1, chunk2), List.of(vec1, vec2));
    assertEquals(2, store.count());
    assertTrue(store.exists("doc-1"));

    // Search query matching vec1
    float[] queryVec = new float[] {1.0f, 0.0f, 0.0f};
    List<SearchResult> hits = store.search(queryVec, 2, 0.5, SearchFilter.empty());

    assertEquals(1, hits.size());
    SearchResult top = hits.get(0);
    assertEquals("chunk-1", top.chunk().id());
    assertEquals(1.0, top.similarityScore(), 0.001);
    assertEquals(1, top.rank());

    // Search with species filter POULTRY
    List<SearchResult> poultryHits =
        store.search(queryVec, 2, 0.0, SearchFilter.ofSpecies("POULTRY"));
    assertEquals(1, poultryHits.size());
    assertEquals("chunk-2", poultryHits.get(0).chunk().id());
  }

  @Test
  void testDeleteAndClear() {
    KnowledgeChunk chunk =
        new KnowledgeChunk("c1", "doc-1", "Title", 0, "Content", 10, Map.of());
    store.upsert(List.of(chunk), List.of(new float[] {1.0f}));

    assertTrue(store.exists("doc-1"));
    store.delete("doc-1");
    assertFalse(store.exists("doc-1"));
    assertEquals(0, store.count());
  }
}
