package app.vetra.ai.rag.store;

import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.SearchFilter;
import app.vetra.ai.rag.model.SearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Thread-safe, high-performance in-memory vector store utilizing cosine similarity scoring
 * and multi-dimensional metadata filtering. Encapsulates raw vector storage internally.
 */
@Component
public class InMemoryVectorStore implements VectorStore {

  /** Internal indexed entry wrapping domain chunk with its dense embedding vector. */
  private record IndexedEntry(KnowledgeChunk chunk, float[] vector) {}

  private final Map<String, List<IndexedEntry>> documentIndex = new ConcurrentHashMap<>();

  @Override
  public void upsert(List<KnowledgeChunk> chunks, List<float[]> vectors) {
    if (chunks == null || vectors == null || chunks.isEmpty()) {
      return;
    }
    if (chunks.size() != vectors.size()) {
      throw new IllegalArgumentException(
          "Chunks count (" + chunks.size() + ") does not match vectors count (" + vectors.size() + ")");
    }

    for (int i = 0; i < chunks.size(); i++) {
      KnowledgeChunk chunk = chunks.get(i);
      float[] vector = vectors.get(i);
      documentIndex
          .computeIfAbsent(chunk.documentId(), k -> new ArrayList<>())
          .add(new IndexedEntry(chunk, vector));
    }
  }

  @Override
  public List<SearchResult> search(
      float[] queryVector, int topK, double minSimilarity, SearchFilter filter) {

    if (queryVector == null || queryVector.length == 0 || topK <= 0) {
      return List.of();
    }

    List<ScoredEntry> scored = new ArrayList<>();

    for (List<IndexedEntry> entries : documentIndex.values()) {
      for (IndexedEntry entry : entries) {
        if (!matchesFilter(entry.chunk(), filter)) {
          continue;
        }

        double similarity = cosineSimilarity(queryVector, entry.vector());
        if (similarity >= minSimilarity) {
          scored.add(new ScoredEntry(entry.chunk(), similarity));
        }
      }
    }

    // Sort descending by similarity
    scored.sort(Comparator.comparingDouble(ScoredEntry::similarity).reversed());

    int limit = Math.min(topK, scored.size());
    List<SearchResult> results = new ArrayList<>(limit);

    for (int rank = 1; rank <= limit; rank++) {
      ScoredEntry s = scored.get(rank - 1);
      results.add(new SearchResult(s.chunk(), s.similarity(), rank));
    }

    return results;
  }

  @Override
  public void delete(String documentId) {
    if (documentId != null) {
      documentIndex.remove(documentId);
    }
  }

  @Override
  public boolean exists(String documentId) {
    return documentId != null && documentIndex.containsKey(documentId);
  }

  @Override
  public long count() {
    return documentIndex.values().stream().mapToInt(List::size).sum();
  }

  @Override
  public void clear() {
    documentIndex.clear();
  }

  /** Checks whether chunk metadata satisfies all specified filter criteria. */
  private boolean matchesFilter(KnowledgeChunk chunk, SearchFilter filter) {
    if (filter == null) {
      return true;
    }

    Map<String, String> meta = chunk.metadata() != null ? chunk.metadata() : Map.of();

    if (!matchesString(meta.getOrDefault("species", ""), filter.species())) {
      return false;
    }
    if (!matchesString(meta.getOrDefault("category", meta.getOrDefault("diseaseCategory", "")), filter.diseaseCategory())) {
      return false;
    }
    if (!matchesString(meta.getOrDefault("documentType", meta.getOrDefault("format", "")), filter.documentType())) {
      return false;
    }
    if (!matchesString(meta.getOrDefault("source", ""), filter.source())) {
      return false;
    }

    return matchesCustomFilters(meta, filter.customFilters());
  }

  private boolean matchesString(String actual, String expected) {
    if (expected == null || expected.isBlank()) {
      return true;
    }
    return actual.equalsIgnoreCase(expected);
  }

  private boolean matchesCustomFilters(Map<String, String> meta, Map<String, String> custom) {
    if (custom == null || custom.isEmpty()) {
      return true;
    }
    for (Map.Entry<String, String> entry : custom.entrySet()) {
      String val = meta.get(entry.getKey());
      if (val == null || !val.equalsIgnoreCase(entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  /** Computes cosine similarity between two float vectors. */
  private double cosineSimilarity(float[] v1, float[] v2) {
    if (v1.length != v2.length) {
      return 0.0;
    }

    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (int i = 0; i < v1.length; i++) {
      dotProduct += v1[i] * v2[i];
      norm1 += v1[i] * v1[i];
      norm2 += v2[i] * v2[i];
    }

    if (norm1 <= 0.0 || norm2 <= 0.0) {
      return 0.0;
    }

    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
  }

  private record ScoredEntry(KnowledgeChunk chunk, double similarity) {}
}
