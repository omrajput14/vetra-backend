package app.vetra.ai.rag.model;

import java.time.Instant;
import java.util.Map;

/**
 * Domain model representing a veterinary medical literature document or clinical protocol.
 *
 * @param id unique document identifier
 * @param title clinical title or publication name
 * @param author author, organization, or veterinary committee
 * @param category disease or clinical domain (e.g. EPIDEMIOLOGY, PHARMACOLOGY)
 * @param rawContent complete unparsed text content
 * @param format document format (e.g. TEXT, MARKDOWN, PDF)
 * @param metadata arbitrary document-level metadata
 * @param indexedAt ingestion timestamp
 */
public record KnowledgeDocument(
    String id,
    String title,
    String author,
    String category,
    String rawContent,
    String format,
    Map<String, String> metadata,
    Instant indexedAt) {

  /** Convenience factory method for plain text or markdown document ingestion. */
  public static KnowledgeDocument of(
      String id,
      String title,
      String author,
      String category,
      String rawContent,
      String format,
      Map<String, String> metadata) {
    return new KnowledgeDocument(
        id, title, author, category, rawContent, format, metadata, Instant.now());
  }
}
