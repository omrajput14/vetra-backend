package app.vetra.ai.rag.model;

/**
 * Structured citation metadata attributing clinical answers to indexed literature.
 *
 * @param documentTitle human-readable title of the source document
 * @param chunkId identifier of the specific retrieved chunk
 * @param source publishing entity or reference catalog
 * @param similarityScore cosine similarity score (0.0 to 1.0)
 */
public record Citation(
    String documentTitle,
    String chunkId,
    String source,
    double similarityScore) {
}
