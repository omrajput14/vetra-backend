package app.vetra.ai.rag.model;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates synthesized context retrieved from the veterinary knowledge base along with
 * structured citations for explainable AI and source attribution.
 *
 * @param contextText combined, formatted textual context for prompt injection
 * @param citations list of structured source citations
 * @param totalChunks count of retrieved chunks
 * @param totalContextTokens estimated token length of the retrieved context
 * @param avgSimilarityScore average cosine similarity score of the retrieved chunks
 */
public record RetrievedContext(
    String contextText,
    List<Citation> citations,
    int totalChunks,
    int totalContextTokens,
    double avgSimilarityScore) {

  /** Creates an empty context representation when no relevant knowledge is found. */
  public static RetrievedContext empty() {
    return new RetrievedContext("", Collections.emptyList(), 0, 0, 0.0);
  }

  /** Returns true if relevant context was successfully retrieved. */
  public boolean hasContext() {
    return totalChunks > 0 && contextText != null && !contextText.isBlank();
  }
}
