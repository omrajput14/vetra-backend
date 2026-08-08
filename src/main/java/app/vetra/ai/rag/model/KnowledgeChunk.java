package app.vetra.ai.rag.model;

import java.util.Map;

/**
 * Pure domain model representing a segmented chunk of a veterinary clinical document.
 * Raw embedding vectors are encapsulated internally inside the VectorStore.
 *
 * @param id unique identifier of the chunk
 * @param documentId parent document identifier
 * @param documentTitle title of the parent document
 * @param chunkIndex zero-based index of this chunk within the document
 * @param content raw text content of the chunk
 * @param tokenCount approximate token length
 * @param metadata contextual metadata (species, disease, category, source, etc.)
 */
public record KnowledgeChunk(
    String id,
    String documentId,
    String documentTitle,
    int chunkIndex,
    String content,
    int tokenCount,
    Map<String, String> metadata) {
}
