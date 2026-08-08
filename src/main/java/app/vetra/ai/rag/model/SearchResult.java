package app.vetra.ai.rag.model;

/**
 * Domain representation of a vector similarity search hit.
 *
 * @param chunk the retrieved knowledge chunk
 * @param similarityScore cosine similarity score between 0.0 and 1.0
 * @param rank 1-based ranking position in retrieval results
 */
public record SearchResult(
    KnowledgeChunk chunk,
    double similarityScore,
    int rank) {
}
